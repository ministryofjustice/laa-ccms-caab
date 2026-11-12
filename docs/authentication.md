# Authentication

Users sign in with OpenID Connect against Microsoft EntraID. This describes what happens after
they do, how to run it locally, and what has to be configured in EntraID for it to work.

## Why there is more to it than signing in

EntraID authenticates a person and tells us who they are as an email address. Nothing in this
application works in those terms: user functions, the user details on the model and session, and
every EBS and SOA call made on a user's behalf are all keyed by a CCMS username. A user who has
signed in successfully is therefore still not a user we know anything about, and looking them up
by the address EntraID gave us finds nobody.

Two lookups close that gap. Which one applies depends on the tenant the application is deployed
against, and is chosen by `IS_SILAS_ENABLED`:

| | `IS_SILAS_ENABLED=false` (internal) | `IS_SILAS_ENABLED=true` (external) |
|---|---|---|
| Who | Caseworkers, signing in as themselves | Providers, federated in through SiLAS |
| Identity on the token | `@justice.gov.uk` email address | SiLAS identifier in a custom claim |
| Resolved by | EBS `XXCCMS_ENTRA_ID_USERS` mapping table | the SiLAS uuid on the `LAA_CCMS_USERNAME` claim, exchanged through the User Details API |
| Call made | `GET /users/entra-mapping?entra-email=...` | `GET /api/v1/user-details/silas/{id}` |

The legacy PUI makes exactly the same distinction off the same two settings, in
`RetrieveUserInfoFromPortal`.

### How it fits together

`CcmsUserIdentityResolver` runs during login, from the `OidcUserService` configured in
`SecurityConfiguration`. It returns the CCMS username, which is then used to load the user's
functions from EBS and is wrapped up as a `CcmsOidcUser` - an authenticated principal whose
`getName()` answers with the CCMS username rather than the EntraID one. Everything downstream is
unchanged as a result: `OidcPrincipalControllerAdvice` and the route rules still see the CCMS user
they always saw.

If the identity cannot be resolved, or the CCMS user cannot be loaded from EBS, login fails with a
`CcmsUserResolutionException`. The user is redirected to `/authentication-error`, is told they
cannot sign in, and is left unauthenticated - they are never partially admitted.

### Which claim carries the EntraID identity

EntraID only issues the `email` claim when the app registration asks for it as an optional claim.
`preferred_username` carries the user principal name, which for an organisational account is the
same address, so it is used when `email` is absent. If neither is present, login fails rather than
guessing.

## Running locally

Local development authenticates against the
[OIDC mock server](https://github.com/ministryofjustice/laa-oidc-mock-server), which docker-compose
builds from a clone alongside this repository and serves on port 9000.

Its `Dockerfile` copies in a jar rather than building one, so build the jar first. Use `assemble`
rather than `build` - the repository's `checkstyleMain` task currently fails on a missing config
file.

```shell
cd ../laa-oidc-mock-server && ./gradlew clean assemble && cd -

docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-oidc-mock
```

Sign in as `provider.user@provider.com` with password `password`. `application-local.yml` already
points at it, so nothing else is needed.

Start the mock **before** this application. `issuer-uri` makes spring fetch the provider's
discovery document while it builds the client registration, so the application will not start at
all if the IdP is unreachable - locally or in a deployed environment.

The mock registers a single redirect URI,
`http://${REDIRECT_BASE_URI}/login/oauth2/code/silas-identity`. Two things follow from that:

- `REDIRECT_BASE_URI` in `docker-compose.yml` is `localhost:8010/civil`, and so carries this
  application's context path as well as its host and port.
- The spring client registration is called `silas-identity`, because the registration id is the
  last segment of the redirect URI. **The redirect URI registered in EntraID has to use the same
  segment.**

`portal.logoutUrl` points at the mock's own logout. Note that the mock answers `/logout` with a
confirmation form rather than clearing its session, so the next sign-in may not prompt; its OIDC
end-session endpoint is `/connect/logout`.

To run against a real EntraID tenant instead, override `OIDC_ISSUER_URI`, `OIDC_CLIENT_ID`,
`OIDC_CLIENT_SECRET` and `PORTAL_LOGOUTURL`. The browser registration reads the `OIDC_*`
credentials, defaulting to the mock's, so setting `AZURE_CLIENT_ID` alone sends the mock's client
to the real tenant and sign-in fails. Without `PORTAL_LOGOUTURL` you are signed out against the
mock and the EntraID session stays open.

## Configuration

| Variable | Required | Description |
|---|---|---|
| `AZURE_TENANT_ID` | yes | EntraID tenant. Builds the issuer, the logout URL and the `entra-api` token endpoint. |
| `AZURE_CLIENT_ID` | yes | This application's own client id, used by the four API registrations. |
| `AZURE_CLIENT_SECRET` | yes | That client's secret. Hold in a secret, never in values. |
| `OIDC_CLIENT_ID` | no (defaults to `AZURE_CLIENT_ID`) | The client the browser sign-in presents. Set it only where `OIDC_ISSUER_URI` points at a mock, which knows its own client and not ours. |
| `OIDC_CLIENT_SECRET` | no (defaults to `AZURE_CLIENT_SECRET`) | That client's secret. |
| `POST_LOGOUT_REDIRECT_URI` | yes | Where EntraID returns the user after signing out. |
| `PORTAL_LOGOUTURL` | no (defaults per profile) | Where the user is sent to sign out. Deployed it is the tenant's logout endpoint; the mock-backed environments and the local profile point it at the mock. Follows whoever signed the user in. |
| `IS_SILAS_ENABLED` | no (default `false`) | `true` for external/SiLAS tenants, `false` for internal. |
| `ENTRA_CUSTOM_USER_ID_CLAIM` | when SiLAS is enabled | Name of the claim SiLAS supplies the user's uuid in - `LAA_CCMS_USERNAME`, a bare name rather than a URI. Unset, or absent from the token, the sign-in is refused: the authenticated email address is not a SiLAS identifier, so there is nothing to exchange. |
| `USER_DETAILS_API_HOSTNAME` | when SiLAS is enabled | Base URL of the User Details API, **including the scheme** - the `url` is the hostname unchanged, deployed and under the local profile alike, so one value works in both. `application-secret.yml` is the exception: it builds the `url` itself from a bare hostname and port, so a value with a scheme there yields `http://https://host:443`, which fails on the first call rather than at startup. |
| `USER_DETAILS_API_ACCESS_TOKEN` | when SiLAS is enabled | Sent as `X-Authorization`. Confirmed against the dev API: `Authorization` returns `401 No API access token provided`, and an invalid value returns `401 Invalid API access token provided`. |

## Calling the backend APIs

The four backend APIs each authenticate with a static access token by default. The
`api-oauth2` profile replaces those with EntraID bearer tokens: it declares one
`client_credentials` registration per API, and `ApplicationConfig` switches an API to bearer tokens
only when a registration matching its id exists. So the profile is the whole switch - adding it to
`SPRING_PROFILES_ACTIVE` moves all four at once, and leaving it off keeps the static tokens.

These registrations use their own provider, `entra-api`, rather than the `silas-identity` one the
browser login uses. Development, test and feature point `silas-identity` at the OIDC mock, and a
token from there is rejected by the APIs, which validate against the real tenant. `entra-api`
names the tenant's token endpoint directly, so a service token comes from EntraID whoever signed
the user in. `OIDC_ISSUER_URI` deliberately does not affect it.

Each registration needs the API's own `<audience>/.default` scope, because EntraID issues an app
role only against the scope of the API that exposes it:

| Variable | Description |
|---|---|
| `EBS_API_SCOPE` | e.g. `api://laa-ccms-data-api/.default` |
| `SOA_API_SCOPE` | e.g. `api://laa-ccms-soa-gateway-api/.default` |
| `CAAB_API_SCOPE` | e.g. `api://laa-ccms-caab-api/.default` |
| `ASSESSMENT_API_SCOPE` | e.g. `api://laa-ccms-caab-assessment-api/.default` |

`AZURE_CLIENT_ID` / `AZURE_CLIENT_SECRET` are reused - the same app registration holds the app role
assignments, so there is no second credential to manage.

**Do not enable the profile before the app roles exist.** A registration whose role has not been
granted still produces a token, which the API then rejects as unauthorised, so turning this on early
breaks every call to that API rather than falling back.

## EntraID app registration

The application is registered as **LAA CCMS Modernised PUI**. Unlike the existing PUI registrations
it uses **OIDC, not SAML**.

- **Redirect URI**: `https://<host>/civil/login/oauth2/code/silas-identity` for each environment.
  The `/civil` comes from `server.servlet.context-path`; the final segment is the spring client
  registration id and must not be changed independently of `application.yml`.
- **Post-logout redirect URI**: the value of `POST_LOGOUT_REDIRECT_URI`. This is where EntraID
  returns the browser after signing the user out, which the application reaches through
  `portal.logoutUrl`. It is not a front-channel logout endpoint: none is implemented, so do not
  register one. EntraID would call it with a GET in an iframe, and the only logout this
  application exposes is Spring's CSRF-protected POST, so such a call would not end the CCMS
  session.
- **Scopes**: `openid`, `profile`, `email`.
- **Optional claim**: add `email` to the ID token, so the internal mapping table lookup has an
  address to work with. Without it the application falls back to `preferred_username`.
- **Groups claim**: only needed if EntraID groups are to be mapped to authorities. Route
  permissions come from EBS user functions, not from EntraID.

### Custom claim for SiLAS

External users need SiLAS to put an identifier on the token. That is a custom claims policy on the
app registration's service principal, driven by the SiLAS custom claims provider. The
`application-registrationV2.2.0` module in
[staff-identity-idam-entra-infra](https://github.com/ministryofjustice/staff-identity-idam-entra-infra)
supports this directly:

```hcl
silas_custom_claims                 = ["LAA_CCMS_USERNAME"]
silas_custom_claims_provider_tenant = "ExternalTest" # "ExternalProd" for prod
```

The claim name in the JWT is the value in the list, so `ENTRA_CUSTOM_USER_ID_CLAIM` is set to the
same string.

Despite its name the claim carries a **SiLAS uuid, not a username**, so it is always exchanged
through the User Details API - the same shape the legacy PUI uses. Verified against Dev External:
uuid `9230231d-...` returns `userLoginId` `OLUWA-SOWUN-SWITA-SOL-LTD`.

The module's allowed claim names do not yet include `LAA_CCMS_USERNAME`.

The dev User Details API has been verified reachable and authenticating
(`https://laa-ccms-user-details-api-dev.apps.live.cloud-platform.service.justice.gov.uk`), with an
unknown identifier returning 404 - which this application treats as "no such user" rather than a
failure.

### Still to do

- Create the app registration manually in **Dev External**, then via terraform for NLE External
  Test, Preprod and Prod.
- Deploy
  [laa-ccms-caab-helm-charts#303](https://github.com/ministryofjustice/laa-ccms-caab-helm-charts/pull/303),
  which supplies the variables in the table above and sets `SPRING_PROFILES_ACTIVE=api-oauth2`.
  Until it is deployed no environment can authenticate.
- Grant the app role to this application in each environment's tenant, and add the six scope and
  User Details API keys to that environment's UI secret. The profile is set for every environment
  rather than enabled one at a time, because the four APIs accept nothing but a bearer token - so
  an environment missing the role or the scopes does not fall back, it fails every API call.
- The EBS `/users/entra-mapping` endpoint has to be released in
  [laa-ccms-data-api](https://github.com/ministryofjustice/laa-ccms-data-api) before the internal
  path works.
