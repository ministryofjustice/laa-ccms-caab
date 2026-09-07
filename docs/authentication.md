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
| Resolved by | EBS `XXCCMS_ENTRA_ID_USERS` mapping table | User Details API |
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

`portal.logoutUrl` points at the mock's own logout, so signing out ends the session at the IdP too
and the next sign-in prompts again rather than silently reusing it.

To run against a real EntraID tenant instead, override `OIDC_ISSUER_URI`, `AZURE_CLIENT_ID` and
`AZURE_CLIENT_SECRET`.

## Configuration

| Variable | Required | Description |
|---|---|---|
| `AZURE_TENANT_ID` | yes | EntraID tenant, used to build the issuer and logout URLs. |
| `AZURE_CLIENT_ID` | yes | App registration client id. |
| `AZURE_CLIENT_SECRET` | yes | App registration client secret. Hold in a secret, never in values. |
| `POST_LOGOUT_REDIRECT_URI` | yes | Where EntraID returns the user after signing out. |
| `IS_SILAS_ENABLED` | no (default `false`) | `true` for external/SiLAS tenants, `false` for internal. |
| `ENTRA_CUSTOM_USER_ID_CLAIM` | when SiLAS is enabled | Name of the claim carrying the SiLAS identifier. When unset or absent from the token, the authenticated identity is used unchanged. |
| `USER_DETAILS_API_HOSTNAME` | when SiLAS is enabled | Base URL of the User Details API. |
| `USER_DETAILS_API_ACCESS_TOKEN` | when SiLAS is enabled | Sent as `X-Authorization`, as the legacy PUI sends it. |

## EntraID app registration

The application is registered as **LAA CCMS Modernised PUI**. Unlike the existing PUI registrations
it uses **OIDC, not SAML**.

- **Redirect URI**: `https://<host>/civil/login/oauth2/code/silas-identity` for each environment.
  The `/civil` comes from `server.servlet.context-path`; the final segment is the spring client
  registration id and must not be changed independently of `application.yml`.
- **Front-channel logout URL**: the value of `POST_LOGOUT_REDIRECT_URI`.
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
silas_custom_claims                 = ["CCMS_USERNAME"]
silas_custom_claims_provider_tenant = "ExternalTest" # "ExternalProd" for prod
```

The claim name in the JWT is the value in the list, so `ENTRA_CUSTOM_USER_ID_CLAIM` is set to the
same string.

**Open question for IDAM and SiLAS.** The module's allowed claims are `LAA_ACCOUNTS`,
`LAA_APP_ROLES`, `USER_EMAIL`, `USER_NAME`, `FIRM_NAME`, `FIRM_CODE` and `CCMS_USERNAME`. None of
them is named as a SiLAS UUID, and `CCMS_USERNAME` looks like it would carry the CCMS username
itself - which would make the User Details API call unnecessary for external users. This
implementation follows the legacy PUI, which takes an identifier from the claim and exchanges it
through the User Details API. Confirm with IDAM (John Nolan) and SiLAS (Callum Brett) which claim
actually carries the SiLAS identifier before configuring `ENTRA_CUSTOM_USER_ID_CLAIM`; if the
answer is that `CCMS_USERNAME` is the CCMS username, the external path can be shortened to read it
straight off the token.

### Still to do

- Create the app registration manually in **Dev External**, then via terraform for NLE External
  Test, Preprod and Prod.
- Update [laa-ccms-caab-helm-charts](https://github.com/ministryofjustice/laa-ccms-caab-helm-charts).
  It still sets `SPRING_SECURITY_SAML2_*` and `SAML_PRIVATE_KEY` / `SAML_CERTIFICATE`, and sets
  none of the variables in the table above, so no deployed environment can authenticate until it
  is changed.
- The EBS `/users/entra-mapping` endpoint has to be released in
  [laa-ccms-data-api](https://github.com/ministryofjustice/laa-ccms-data-api) before the internal
  path works.
