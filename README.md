
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
./gradlew bootRun --args='--spring.profiles.active=local
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

## Contributing
Follow the [contribution guide](./CONTRIBUTING.md) to make code changes.