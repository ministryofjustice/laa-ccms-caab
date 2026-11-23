# Authorisation boundaries and default-deny

Status: Proposed — requires agreement from Paul K and the API/platform owners.

Related: [CCMS-1460](https://dsdmoj.atlassian.net/browse/CCMS-1460),
[CCMS-1461](https://dsdmoj.atlassian.net/browse/CCMS-1461),
[CCMS-1462](https://dsdmoj.atlassian.net/browse/CCMS-1462).

## Context and implemented guardrails

EntraID OIDC establishes the user's CCMS identity. Application function codes
authorise actions; authentication alone does not establish provider or case access.
The inherited work from [#882](https://github.com/ministryofjustice/laa-ccms-caab/pull/882),
reconciled with current `main` (including outcome submission and discharge routes
from [#960](https://github.com/ministryofjustice/laa-ccms-caab/pull/960)), extends
those URL function rules and template controls:

- **OIDC privilege refresh & session isolation:** Privilege reloads update both
  the Spring Security authentication and the OIDC principal, preserve non-function
  authorities and remove revoked functions. On provider switch, stale
  provider-scoped session state (active case, case, application, notification and
  search attributes) is cleared while preserving session infrastructure; if the
  user cannot be reloaded for the newly selected provider, the session is
  invalidated rather than reusing the previous provider's privileges.
- **Exhaustive route matrix & coverage:** The production `SecurityFilterChain` is
  tested against an independent manifest of every production MVC mapping, HTTP
  method, significant path-variable value (including `caseContext`,
  `billingContext`, `action`, `submissionContext` and `submissionType`) and
  mapping condition. Every `UserRole` function and composite/alternative
  prerequisite is exercised, and any newly added mapping — even beneath an
  existing protected wildcard — fails the build until explicitly classified.
  `SecurityConfiguration` is included in JaCoCo coverage verification again.
  `SubmittedCaseClientInterceptor` logs `AUTHORIZATION_DENIED` before redirecting
  refused submitted-case client edits.
- **Identity-bound assessment confirmation:** Both confirmation callback aliases
  bind the decoded provider and case identifiers to the current session user and
  active case before making any assessment or lookup calls, and query using the
  validated session identifiers. Missing state, malformed tokens, mismatches and
  invalid rulebases are denied and audit-logged.
- **Provider-scoped attachment handling:** Notification attachment removal is a
  function-authorised, CSRF-protected POST (`GET`/`HEAD` return 405), and all
  draft notification attachment operations (create, list, validate, download,
  submit and remove) consistently pass `user.getProvider().getId()` rather than
  `user.getUserId()`.

## Decision 1: runtime default-deny and authenticated-only routes (U1)

**Current state:** `SecurityConfiguration` retains `anyRequest().authenticated()`.
In legacy PUI (`laa-ccms-provider-ui`, `CheckActionAllowed.java`), function checks
were applied only to explicit action transitions, leaving session-bound section
editing authenticated-only. The independent route manifest currently classifies
all discovered routes into explicit function rules, 2 `PUBLIC` routes and the
remaining `AUTHENTICATED` routes.

**Recommendation for Paul K:** Adopt runtime `anyRequest().denyAll()` backed by
an explicit allowlist of genuinely `PUBLIC` routes, agreed `AUTHENTICATED` routes
and required framework paths (`/error`, `/login/**`, `/oauth2/**`, `/logout`).
Because the manifest already enumerates every non-function route, the allowlist is
concrete and low-risk, and fails closed even if a route is introduced outside the
test inventory. Until Paul K approves this change, `anyRequest().authenticated()`
is retained with the exhaustive inventory as a mandatory build check.

**Open question:** Confirm whether `/application/sections/**` and
`/application/abandon` should remain `AUTHENTICATED` (relying on session-bound
draft application state, matching legacy PUI) or require explicit entry functions
such as `CA` / `YCA`.

## Decision 2: URL rules vs method/service-layer authorisation (U2)

**Current state:** Coarse function checks live in `SecurityConfiguration`, while
[#960](https://github.com/ministryofjustice/laa-ccms-caab/pull/960) introduced
targeted service-layer guards (`OutcomeSubmissionService.requireSubmissionPermission`,
`canDischarge`) for state-dependent rules that URL matchers cannot express.

**Recommendation for Paul K:** Keep URL rules + the exhaustive manifest as the
primary mechanism for coarse function checks in this single-entry MVC BFF, paired
with explicit domain/service guards where authorisation depends on case state or
provider ownership. Avoid duplicating URL function checks with global
`@PreAuthorize` annotations: duplicate rules drift easily, and `AccessDeniedException`
thrown from MVC controllers must be coordinated with `GlobalExceptionHandler` so
it is routed through `NotAuthorisedAccessDeniedHandler` and `SecurityEventLogger`
rather than rendered as a generic error page.

**Open question:** Agree this layered boundary and standardise how service-layer
authorisation denials map to refusal navigation and `AUTHORIZATION_DENIED` audit
events.

## Decision 3: service-to-service identity and downstream API authorisation (U3)

**Verified live state (as of Nov 2026):**
- EntraID OAuth2 client-credentials bearer authentication has now merged across
  the UI and all four backend APIs
  ([laa-ccms-caab-api#207](https://github.com/ministryofjustice/laa-ccms-caab-api/pull/207),
  [laa-ccms-data-api#307](https://github.com/ministryofjustice/laa-ccms-data-api/pull/307),
  [laa-ccms-caab-assessment-api#142](https://github.com/ministryofjustice/laa-ccms-caab-assessment-api/pull/142),
  [laa-ccms-soa-gateway-api#321](https://github.com/ministryofjustice/laa-ccms-soa-gateway-api/pull/321))
  and chart configuration
  ([laa-ccms-caab-helm-charts#303](https://github.com/ministryofjustice/laa-ccms-caab-helm-charts/pull/303),
  enabling `SPRING_PROFILES_ACTIVE=api-oauth2` on the UI and removing static
  `authorized-clients`).
- **Remaining gaps:**
  1. The UI authenticates to all four API scopes with a single shared
     `AZURE_CLIENT_ID` / `AZURE_CLIENT_SECRET` holding a broad `ALL` role; no
     per-operation or per-API least-privilege app roles or workload identity
     federation are in place yet.
  2. `USER_DETAILS_API_ACCESS_TOKEN` (`X-Authorization`) and SOA→WebLogic/EBS
     service credentials remain static secrets.
  3. None of the four backend APIs inspect end-user JWT claims or enforce
     end-user provider/resource ownership — they verify only the service bearer
     token and trust caller-supplied `providerId`, `userId`, `Caab-User-Login-Id`
     and `soaGatewayUserRole` parameters.

**Open question for Paul K & API/platform owners:** Agree whether the backend
APIs remain a trusted-BFF boundary (hardened via internal-only network policy,
least-privilege per-caller app roles, and secret rotation/workload identity) or
whether user/provider context must be propagated and enforced independently at
the API layer. Service authentication does not substitute for end-user provider
authorisation.

## Decision 4: OPA confirmation token lifecycle (U4 / CCMS-1462)

**Verified contract in `laa-ccms-connector` and `laa-ccms-provider-ui`:**
- `SecurityUtils.createHubContext` stamps `ttl` once at OPA launch alongside
  `ezgovId`.
- `laa-ccms-connector` (`HubTdsWebService.java`, `application.yml`) enforces
  `ccms.pui.connector.ttl.millis = 20000` (20 s) strictly on **Load** and never
  refreshes `ttl` on **Save**; the `valret` parameter in `ContextToken` is
  unused.
- On completion, OPA redirects the browser to `GET assessments/confirm?val=<original launch token>`,
  which is a read-only confirmation/summary page.
- Imposing a short confirmation TTL would break legitimate 15–60 minute OPA
  interviews without connector changes. Enforcing single-use consumption would
  break browser refresh/back navigation and require shared cross-replica storage
  respecting read-only-root containers for negligible security gain on a
  read-only GET. (Separately, CCMS-1462 tracks replacing legacy `Encryptor.java`
  weak `PBEWithMD5AndDES` encryption.)

**Recommendation for Paul K / OPA owner:** Confirm that session provider/case
binding (implemented here) — optionally paired with a coarse future-timestamp
or session-lifetime sanity check — is sufficient for the read-only confirmation
callback without changing the OPA connector protocol or adding a distributed
replay store.

## Deployment compatibility

[#947](https://github.com/ministryofjustice/laa-ccms-caab/pull/947) (updated at
`584aa6f3` with EntraID/OIDC and agreed **`SameSite=Lax`**, `Secure`, `HttpOnly`
cookies) and
[ministryofjustice/laa-ccms-caab-helm-charts#373](https://github.com/ministryofjustice/laa-ccms-caab-helm-charts/pull/373)
(updated at `7de96a72`) provide separate cookie, browser-header, transport,
container and ingress-exposure controls. They complement, rather than replace,
function and provider authorisation.

API calls use internal Kubernetes services rather than the backend ingresses
disabled by the chart change, and this PR adds no writable filesystem paths or
elevated container privileges. Refusal navigation uses safely recorded local
pages with a context-path `/home` fallback and does not depend on browser
`Referer`; missing/invalid CSRF tokens and background refusals remain logged
403 responses. Browser/environment rollout checks for `#947` + `#373` remain
tracked under CCMS-1459 (with rate-limiting in CCMS-1465 and CSP ingress 403 in
CCMS-1660).
