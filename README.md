
[![Ministry of Justice Repository Compliance Badge](https://github-community.service.justice.gov.uk/repository-standards/api/laa-ccms-caab/badge)](https://github-community.service.justice.gov.uk/repository-standards/laa-ccms-caab)

# laa-ccms-caab-ui

## Local setup

### First time setup

Please follow [first time setup](docs/first-time-setup.md).

### 1. Run dependencies

This application depends on the following docker containers being run when running locally:
- SAML Mock
- Wiremock
- ClamAV
- LocalStack
- Mock Contracts
- SOA proxy (only when running the SOA gateway, PUI or the connector locally against SOA dev)

```shell
docker-compose --compatibility -p laa-ccms-caab-development up -d
```

Run the APIs locally. If you wish to use the APIs running in the DEV environment, you can make use of the `secret` profile
by placing the `application-secret.yml` file in `./src/main/resources/`.

A `application-secret.yml` file can be provided by another developer on 
the [team](https://github.com/orgs/ministryofjustice/teams/laa-ccms-devs).

### 2. Connect to the development EBS database and SOA instance

Start the SSO sessions set up in the first time setup.

### 3. Run the application

The application can be run in one of the following ways. As above, add `secret` to the list of profiles if you are using the APIs in dev.

#### Terminal

```shell
./gradlew bootRun --args='--spring.profiles.active=local'
```

#### IntelliJ

Create a new run configuration for `CaabApplication`, and set the spring profile to `local`.

### 4. Run tests

Unit tests (also included in `build`)

```shell
./gradlew test
```

Integration tests

```shell
./gradlew integrationTests
```

### 5. Styling

This project uses [spotless](https://github.com/diffplug/spotless) and [checkstyle](https://checkstyle.sourceforge.io/) to manage styling. The `spotlessCheck` task will scan for styling issues during `build`. Run the following command to fix styling issues automatically.

```shell
./gradlew spotlessApply
```

## Logging

The service writes its console logs as [Elastic Common Schema (ECS)](https://www.elastic.co/docs/reference/ecs)
JSON, in line with the LAA monitoring and logging guardrail. This is configured under
`logging.structured` in [`application.yml`](./src/main/resources/application.yml); there is deliberately no
`logback-spring.xml`, as one would override this configuration.

### Running locally

The `local` profile (see [Run the application](#3-run-the-application)) switches structured logging off in favour of a readable console pattern, which
still carries the `[traceId,spanId,sessionID]` of the current request:

```shell
./gradlew bootRun --args='--spring.profiles.active=local'
```

```text
2026-10-01T23:48:49.842+01:00 DEBUG 12345 --- [omcat-handler-0] [6abee351946ef81007d2479ebb8f5c32,07d2479ebb8f5c32,9f5de6e6-b841-4398-932d-d95fb651748c] o.s.security.web.FilterChainProxy        : Securing GET /home
```

### Structured (ECS) output

With any other profile, each log event is a single line of JSON. For example (formatted for
readability):

```json
{
  "@timestamp": "2026-10-01T22:48:43.496838Z",
  "log": {
    "level": "DEBUG",
    "logger": "org.springframework.security.web.FilterChainProxy"
  },
  "process": {
    "pid": 12345,
    "thread": {
      "name": "tomcat-handler-0"
    }
  },
  "service": {
    "name": "laa-ccms-caab",
    "version": "1.4.2",
    "environment": "dev",
    "node": {
      "name": "laa-ccms-caab-ui-6f9b8c7d5-k8j3h"
    }
  },
  "message": "Securing GET /home",
  "traceId": "6abee34b8510dcb8f0786f6befb45288",
  "spanId": "f0786f6befb45288",
  "sessionID": "9f5de6e6-b841-4398-932d-d95fb651748c",
  "tags": [
    "COMMONS-LOGGING"
  ],
  "ecs": {
    "version": "8.11"
  }
}
```

| Field                 | Source                                                                                   |
|-----------------------|------------------------------------------------------------------------------------------|
| `service.name`        | `spring.application.name`                                                                |
| `service.version`     | The Gradle project version, substituted for `@version@` in `application.yml` at build time |
| `service.environment` | The active Spring profiles (`SPRING_PROFILES_ACTIVE`), or `default` when none is set     |
| `service.node.name`   | The `HOSTNAME` environment variable, which is the pod name in Kubernetes                 |
| `traceId`, `spanId`   | Micrometer Tracing (Brave), for log events written while a request is being handled      |

### Log levels

| Environment variable   | Logger                | Default |
|------------------------|-----------------------|---------|
| `ROOT_LOGGING_LEVEL`   | root                  | `info`  |
| `SPRING_LOGGING_LEVEL` | `org.springframework` | `info`  |
| `APP_LOGGING_LEVEL`    | `uk.gov.laa.ccms.caab` | `info`  |

## Contributing
Follow the [contribution guide](./CONTRIBUTING.md) to make code changes.

### Request correlation and verification

`sessionID` is a generated opaque logging ID, never the authentication session cookie.
Requests with an existing HTTP session reuse its logging ID; stateless requests get a new ID
without creating a session. It is scoped to this service and is not propagated downstream.
`traceId` and `spanId` are supplied by Micrometer while a request is handled; startup logs
have no request correlation. The local pattern includes all three fields.

Run the focused checks with `./gradlew test --tests '*LoggingAcceptanceTest' --tests '*SessionCorrelationFilterTest'`.
They use synthetic requests and write actual captured log lines to `build/logging-evidence/`.
Do not enable request payload or SQL bind parameter logging with real personal data.

Captured synthetic-request screenshots and validation scope: [logging evidence](docs/logging/README.md).
