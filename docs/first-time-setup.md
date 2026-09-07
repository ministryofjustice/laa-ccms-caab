# First time setup

## 1. Clone required repositories

Clone this repository, along with the following repositories into the same directory

- [laa-ccms-caab-api](https://github.com/ministryofjustice/laa-ccms-caab-api)
- [laa-ccms-caab-ebs-api](https://github.com/ministryofjustice/laa-ccms-data-api)
- [laa-ccms-caab-soa-api](https://github.com/ministryofjustice/laa-ccms-soa-gateway-api)
- [laa-ccms-caab-assessment-api](https://github.com/ministryofjustice/laa-ccms-caab-assessment-api)
- [laa-ccms-mock-contracts](https://github.com/ministryofjustice/laa-ccms-mock-contracts)

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

## Authentication (EntraID / OIDC)

Authentication is via OIDC against EntraID. Set the `AZURE_TENANT_ID`, `AZURE_CLIENT_ID` and
`AZURE_CLIENT_SECRET` environment variables to point at a dev EntraID app registration (redirect
URI: `http://localhost:8010/civil/login/oauth2/code/azure`).

There is currently no local OIDC mock IdP (the previous SAML mock container has been removed as
part of the migration to OIDC); setting up a local mock is a follow-up piece of work.

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
