# Log call review (B-2)

Review of the 207 `log.*` calls in `src/main`, looking for personal data, credentials and
whole-object output. Calls that print only literals, identifiers (case, client and application
references, ids, transaction ids) or counts were kept.

## Changed

| Location | Was | Decision |
|---|---|---|
| `OpponentService` organisation search | Logged the full search criteria at debug | Removed |
| `CounselSearchController` select counsel | Logged the counsel name at debug | Replaced with the result index |
| `AssessmentService` create assessment | Logged `assessment.toString()` at debug | Logs the assessment name only |
| `CspReportController` unrecognised or unparseable report | Logged the raw request body at WARN | Logs the body length only |
| `CspReportController` violation | Logged URIs with query strings and the script sample | Query strings stripped, script sample removed |
| `AbstractApiClientErrorHandler`, `SoaApiClient.updateCase` | Logged the full upstream error body | Kept (it names the invalid field), masked and truncated with `LogMasker` |
| `AddressService`, `NotificationSearchUtil` | Logged `e.getMessage()`, which can echo input | Log the exception class only |
| `ContextUrlEncoder` deflate and inflate | Logged the OPA context token before and after encoding | Removed |
| `SecurityUtils.createHubContext` | Logged the hub return URL | Removed |

`LogMasker` masks email addresses, National Insurance numbers, dates and long digit runs.

## Kept

- `ClientService` no longer logs search criteria (it logs a fixed message), and logs client
  references and transaction ids.
- `AmendmentService` diagnostics, `AssessmentService` and `AssessmentUtil`: case references,
  rulebase and entity names, counts and statuses. The `AssessmentUtil` attribute value is a case
  reference being compared.
- `PuiMetricService`, `AwardController`, `CaseController`: case references and ids.
- `LoggingInterceptor`: method and path only, never the query string.
- `ContextToken` TTL errors: timings only.

## Not covered

- Exception stack traces passed as the last argument can still carry values from upstream
  messages. They are not masked.
- Paths contain case and client references. These are identifiers, and are needed for the
  data-access events.

## Debug logging

`application.yml` sets the root, Spring and application levels to `info` unless
`ROOT_LOGGING_LEVEL`, `SPRING_LOGGING_LEVEL` or `APP_LOGGING_LEVEL` is set. The deployed values
live in the helm chart and environment config, which are not in this repository, so it still
needs confirming that none of them set `debug` in preprod or production.
