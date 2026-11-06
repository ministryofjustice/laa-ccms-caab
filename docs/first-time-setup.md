# First time setup

## 1. Clone required repositories

Clone this repository, along with the following repositories into the same directory

- [laa-ccms-caab-api](https://github.com/ministryofjustice/laa-ccms-caab-api)
- [laa-ccms-caab-ebs-api](https://github.com/ministryofjustice/laa-ccms-data-api)
- [laa-ccms-caab-soa-api](https://github.com/ministryofjustice/laa-ccms-soa-gateway-api)
- [laa-ccms-caab-assessment-api](https://github.com/ministryofjustice/laa-ccms-caab-assessment-api)
- [laa-ccms-mock-contracts](https://github.com/ministryofjustice/laa-ccms-mock-contracts)
- [laa-oidc-mock-server](https://github.com/ministryofjustice/laa-oidc-mock-server)

## 2. Install Java

Follow the steps [here](https://ministryofjustice.github.io/laa-java-community-technical-guidance/java-setup.html#get-started-with-java-versions-and-intellij) (SDKMAN! recommended).

## 3. Build and run dependency images

```shell
docker-compose --compatibility -p laa-ccms-caab-development up -d --build
```

## 4. Complete AWS SSO setup

Follow [AWS SSO Setup](https://dsdmoj.atlassian.net/wiki/spaces/CCMS/pages/6045630839/PUI+Developer+Onboarding).

This is required to connect to the development EBS database and SOA instances.

## 5. Run the APIs

Follow the README for the following APIs to get them running locally

- CAAB API
- CAAB Assessment API
- CAAB SOA Gateway API
- CAAB EBS (Data) API

You can now [run the application](../README.md#3-run-the-application).

Below is further information about all dependencies.

## Set up laa-oidc-mock-server

Authentication is via OIDC. Locally this runs against the
[OIDC mock server](https://github.com/ministryofjustice/laa-oidc-mock-server), which replaces the
SAML mock used before the migration to EntraID. docker-compose builds its image from the clone
alongside this repository and serves it on port 9000.

Its `Dockerfile` copies in a jar rather than building one, so the jar has to be built first - this
is the OIDC equivalent of the `mvn package` step the SAML mock needed. Use `assemble` rather than
`build`: the repository's `checkstyleMain` task currently fails on a missing config file.

```shell
cd ../laa-oidc-mock-server
./gradlew clean assemble
cd -

docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-oidc-mock
```

The mock has to be running **before** this application starts. `issuer-uri` makes spring fetch the
provider's discovery document while it builds the client registration, so the application will not
start if the IdP is unreachable.

Sign in as `provider.user@provider.com` with password `password`.

The mock registers one redirect URI, `http://localhost:8010/civil/login/oauth2/code/silas-identity`,
which is why `REDIRECT_BASE_URI` in `docker-compose.yml` carries this application's context path.

See [authentication](authentication.md) for how a signed-in identity is turned into a CCMS user,
and for running against a real EntraID tenant instead.

## Wiremock standalone

This is required due to a dependency on the ordinance survey api, instead of calling the real thing
we call this.
The wiremock can handle a postcode request.

```shell
docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-wiremock
```

## ClamAV

To facilitate local virus scan when uploading files in the UI, a ClamAV container can be started
using
the following command:

```shell
docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-clam-av
```

## LocalStack (AWS)

LocalStack provides lightweight instances of AWS components, such as S3. When running locally, it is
recommended to
install [LocalStack Desktop](https://docs.localstack.cloud/user-guide/tools/localstack-desktop/)
to monitor and manage components. [LocalStack AWS CLI (
`awslocal`)](https://docs.localstack.cloud/user-guide/integrations/aws-cli/#localstack-aws-cli-awslocal)
can also be useful if more in-depth interactions are required.

**Note: persistence is a pro feature, so files in S3 will not endure on container shutdown.**

A configured LocalStack container can be started via docker compose:

```shell
docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-localstack
```

An S3 bucket `laa-ccms-documents` will be created on startup if it does not already exist, via
[`/localstack/init-s3.sh`](../localstack/init-s3.sh).

## SOA proxy

SOA dev only accepts HTTPS. PUI, the SOA gateway and the connector expect plain
HTTP on `localhost:8051`, and an SSM port-forward alone cannot bridge that: Java
would reject SOA's certificate (a private CA) and its name (not `localhost`).

`laa-ccms-soa-proxy` listens on `localhost:8051` and does the HTTPS to the SOA
port-forward, which therefore has to use `localPortNumber` **8052**. The
port-forward command is on the
[PUI Developer Onboarding](https://dsdmoj.atlassian.net/wiki/spaces/CCMS/pages/6045630839/PUI+Developer+Onboarding)
page. Nothing changes in the apps, their local config, the JDK or `/etc/hosts`.

This needs Docker Desktop: the SSM port-forward listens on the host's loopback
only, and Docker Desktop is what makes `host.docker.internal` reach it from a
container. On Docker Engine (Linux) the proxy would need host networking instead.

The proxy starts with the rest of the stack, or on its own:

```shell
docker-compose --compatibility -p laa-ccms-caab-development up -d laa-ccms-soa-proxy
```

With the port-forward up, `./soa-proxy/soa-tls-check.sh` checks the tunnel,
SOA and the proxy in turn and says what is missing. `docker logs
laa-ccms-soa-proxy` shows why a call was refused.

The proxy does not verify SOA's certificate: the SSM session already
authenticates the instance and target, and the only thing that could stand in
for SOA is a process on your own machine listening on 8052.

## View metrics

This project exposes actuator endpoints, which are scraped by a Prometheus instance. For debugging,
you can run a Prometheus instance locally within Docker:

```shell
docker-compose --compatibility -p laa-ccms-caab-development up -d laa-ccms-caab-prometheus
```

This instance is already setup to scrape the `/actuator/prometheus` endpoint of the various CAAB
services. To access the local Prometheus instance, visit http://localhost:9090.

## OPA/OIA and connector

The OPA/OIA and connector services are not required for the ui to start up locally, but if you want
to test / develop features for the integration then this is required.

This [connector guide](https://github.com/ministryofjustice/laa-ccms-connector/blob/main/documentation/gradle-docker-build.md)
can be followed to get the connector/opa/oia up and running.
