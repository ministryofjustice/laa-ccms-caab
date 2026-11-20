# Authorisation boundaries and default-deny

Status: Proposed; requires agreement from Paul K and the API/platform owners.

Related: [CCMS-1460](https://dsdmoj.atlassian.net/browse/CCMS-1460),
[CCMS-1461](https://dsdmoj.atlassian.net/browse/CCMS-1461),
[CCMS-1462](https://dsdmoj.atlassian.net/browse/CCMS-1462).

## Context and implemented guardrails

EntraID OIDC establishes the user's CCMS identity. Application function codes
authorise actions; authentication alone does not establish provider or case access.
The inherited work from [#882](https://github.com/ministryofjustice/laa-ccms-caab/pull/882)
extends those function rules and presentation controls. Privilege refresh must
update both the authentication and OIDC principal, preserve non-function
authorities and revoke old functions. A provider switch that cannot reload the
selected provider ends the session rather than reusing the previous provider's
privileges.

The production filter chain is tested against an independent inventory of every
production MVC mapping, method, significant path-variable value and mapping
condition. Every function and composite prerequisite is exercised. A new mapping
under an existing protected wildcard must still receive an explicit expectation.
SecurityConfiguration is included in coverage again. This is a build-time
guardrail, not runtime default-deny or proof of controller-level data access.

Assessment confirmation binds decoded provider and case identifiers to the
current session before assessment or lookup calls. Missing state, malformed
tokens, mismatches and invalid rulebases are denied. Backend queries use the
validated session identifiers. Attachment removal is a provider-scoped,
function-authorised POST with CSRF protection; GET/HEAD cannot delete.

## Pending decision: runtime default-deny

Current behaviour retains `anyRequest().authenticated()`. Authenticated-only
journeys are explicitly identified in the independent test inventory without
inventing business function requirements.

Preferred option for discussion: enumerate genuinely public and authenticated-only
flows, retain the agreed function rules, then use `anyRequest().denyAll()`. This
fails closed for routes introduced outside the test inventory, but requires
business agreement on each currently authenticated-only flow and verification of
framework/error/operational routes.

Alternative: retain the authenticated fallback with the exhaustive inventory as
a mandatory build check. This has lower migration risk but cannot protect an
untested deployment or a route missed by discovery.

Paul K must agree the option and the intended authenticated-only flows. Neither
option has been approved by this ADR.

## Pending decision: method and service boundaries

URL rules protect browser requests, not calls made through another entry point.
Sensitive writes and provider-scoped reads may also need method/service guards.
Options are agreed `@PreAuthorize` guards with method security enabled, or
explicit service-boundary checks that have access to authoritative provider/case
state. Choose the ownership boundary and denial behaviour before enabling a
global annotation policy. Duplicate, independently maintained role rules can
diverge; tests must cover direct invocation and alternate entry points if guards
are introduced.

Client-credentials support is separate from deployment readiness and end-user
authorisation. API/platform owners must confirm role grants, distinct caller
identities, credential storage/rotation and token audiences. APIs must still
validate provider/resource scope rather than trust an arbitrary provider ID
because an authenticated service supplied it. The UI cannot substitute for these
downstream checks. No API role activation or credential rotation is performed here.

## Pending decision: OPA confirmation lifecycle

`SecurityUtils.createHubContext` stamps `ttl` at launch and includes `ezgovId`.
`ContextToken` comments distinguish connector-load timing from a possible
save/return token; `isTtlExpired` has no production caller. These do not establish
a confirmation lifetime or atomic single-use contract. CCMS-1462 does not supply
the missing policy.

The OPA owner must establish whether save refreshes the timestamp, the meaning
and uniqueness of the journey identifier, lifetime and clock skew, refresh,
retry and multiple-tab behaviour, and when consumption becomes irreversible.
Consumption before a recoverable backend failure could prevent a legitimate retry.
A replay store must work atomically across replicas and restarts; neither an
in-memory set nor ingress stickiness establishes that guarantee.

Provider/case binding is enforced now. Expiry and single-use remain unimplemented;
neither the 60-minute idle session timeout nor the absolute session timeout is
an agreed OPA token lifetime. Raw session IDs are not used as replay keys.

## Deployment compatibility

[#947](https://github.com/ministryofjustice/laa-ccms-caab/pull/947) and
[ministryofjustice/laa-ccms-caab-helm-charts#373](https://github.com/ministryofjustice/laa-ccms-caab-helm-charts/pull/373)
provide separate cookie, browser-header, transport, container and exposure controls.
They do not replace function or provider authorisation.

API calls continue to use internal services, not the backend ingresses disabled
by the chart change. This implementation adds no writable storage or privileged
container requirements. Future replay storage must respect non-root,
read-only-root deployment constraints.

Refusal navigation uses safely recorded local pages, with a context-path home
fallback, and does not require a full Referer. Missing/invalid CSRF tokens and
background refusals remain logged 403 responses. Real EntraID callback and OPA
round-trip evidence under Strict cookies, plus regression HTTPS DNS/TLS/allowlist
reachability, remain the existing CCMS-1459 deployment gates. Unit/render tests
cannot establish those browser or environment properties.
