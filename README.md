# laa-ccms-caab-ui

## How to run this application:

The laa-ccms-caab-ui requires multiple other microservices in order to run locally and function
correctly. They are:

- [laa-ccms-caab-api](https://github.com/ministryofjustice/laa-ccms-caab-api)
- [laa-ccms-caab-ebs-api](https://github.com/ministryofjustice/laa-ccms-data-api)
- [laa-ccms-caab-soa-api](https://github.com/ministryofjustice/laa-ccms-soa-gateway-api)
- [laa-ccms-caab-saml-mock](https://github.com/ministryofjustice/laa-ccms-caab-saml-mock)
- [laa-ccms-caab-db](https://github.com/ministryofjustice/laa-ccms-provider-ui-database)

### Quick setup

This application only depends on 4 docker containers being run when running locally:
- SAML Mock
- Wiremock
- ClamAV
- LocalStack

You can find more information about each of those services below, but if you want to quickly get
started, the below command will start all 4 of these containers.
```shell
docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-saml-mock laa-ccms-caab-wiremock laa-ccms-caab-clam-av laa-ccms-caab-localstack
```

If you wish to use the API's running in the DEV environment, you can make use of the `secret` profile
by placing the `application-secret.yml` file in `./src/main/resources/`. 

A `application-secret.yml` file can be provided by another developer on 
the [team](https://github.com/orgs/ministryofjustice/teams/laa-ccms-devs).

## Feature flags

Feature flags can be used to enabled or disable access to certain features of the application.

### Available feature flags

| Feature Flag | Description                                                                    | Default |
|--------------|--------------------------------------------------------------------------------|---------|
| amendments   | Provides access to all functionality related to amending a case / application. | false   |

### Enabling a feature flag

To enable a feature, or list of features, set the following application property:

```yaml
laa:
  ccms:
    features:
      - feature: amendments
        enabled: true
```

### Creating a new feature flag

To create a feature flag, simply add the name of your feature to the `uk.gov.laa.ccms.caab.feature.Feature` enum.


### Using feature flags

To put a method under a feature flag, use the `@RequiresFeature` annotation as follows.

**Example 1**

If `FEATURE_1` is enabled, the controller method will execute as usual.
Otherwise the user will be redirected to a 'feature unsupported' screen.
```java
  @GetMapping("/resource")
  @RequiresFeature(Feature.FEATURE_1)
  public String allResources(Model model) {

    List<Resource> resources = service.getResources();

    model.addAttribute("resources", resources);

    return "resource_view";
  }
```


**Example 1**

If `FEATURE_1` is enabled AND the requested resource id is `id1`, the controller method will execute as usual.
Otherwise the user will be redirected to a 'feature unsupported' screen.

The `conditionExpression` is a `SpEL` expression which allows granular control over what requests come under the feature, if required.
```java
  @GetMapping("/resource/{resource-id}")
  @RequiresFeature(Feature.FEATURE_1, conditionExpression = "#resourceId == 'id1'")
  public String resource(@PathVariable("resource-id") String resourceId,
                         Model model) {

    Resource resource = service.getResource(resourceId);

    model.addAttribute("resource", resource);

    return "resource_view";
  }
```

**Example 3**

If further granularity is required, the FeatureService can be injected directly. A `isEnabled(Feature feature)` 
method is available to check whether a feature is enabled. This can be used to carry out any conditional business logic.

```java
@Controller
@RequiredArgsConstructor
public class ResourceController {

  private final FeatureService featureService;

  @GetMapping("/resource")
  public String allResources(Model model) {

    List<Resource> resources = service.getResources();

    if (featureService.isEnabled(Feature.FEATURE_1)) {
      // filter resources related to FEATURE_1
    }

    model.addAttribute("resources", resources);

    return "resource_view";
  }
}
```

## Set up laa-ccms-caab-saml-mock

This step requires maven to be installed on your machine. You can
use [homebrew](https://formulae.brew.sh/formula/maven) to install it.

```shell
brew install maven
```

Next steps:

```shell
cd ..

git clone git@github.com:ministryofjustice/laa-ccms-caab-saml-mock.git laa-ccms-caab-saml-mock
cd laa-ccms-caab-saml-mock

mvn -B package --file pom.xml

cp mujina-idp/target/laa-ccms-caab-saml-mock-1.0.0.jar laa-ccms-caab-saml-mock-1.0.0.jar

cd ../laa-ccms-caab

docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-saml-mock

```

### Run laa-ccms-caab-saml-mock standalone

```shell
docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-saml-mock
```

## Setup laa-ccms-caab-db

```shell
cd ..

git clone git@github.com:ministryofjustice/laa-ccms-provider-ui-database.git laa-ccms-caab-db


git clone https://github.com/ministryofjustice/docker-liquibase.git

cd docker-liquibase

docker build -t caab-liquibase .


cd ..

cd laa-ccms-caab

docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-db laa-ccms-caab-liquibase
```

Now wait 10 mins for the db to be populated ia the liquidbase scripts.
Have a look at the container logs to check its progress.

```
Liquibase 'updateSql' Successful
```

When you see this message you can stop the liquibase container.

### Run laa-ccms-caab-db standalone

```shell
docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-db
```

## Wiremock standalone

This is required due to a dependency on the ordinance survey api, instead of calling the real thing
we call this.
The wiremock can handle and postcode request.

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
[`/localstack/init-s3.sh`](localstack/init-s3.sh).

## OPA/OIA and connector

The OPA/OIA and connector services are not required for the ui to start up locally, but if you want
to test / develop features for the integration then this is required.

This [connector guide](https://github.com/ministryofjustice/laa-ccms-connector/blob/main/documentation/gradle-docker-build.md)
can be followed to get the connector/opa/oia up and running.

If you are running on an M series mac and using colima. You will most likely need 2 docker
contexts/vms.
One running x86_64 architecture and the other running arm64 architecture.

See guide
below [M series Macbook/Colima development setup](#m-series-macbookcolima-development-setup) for
more information.

## secrets.gradle (required for gradle build)

create a secrets.gradle file in the root directory:

```
project.ext.gitPackageUser = "{your name}"
project.ext.gitPackageKey = "{your personal api token}"
```

Replace the username with your own name, and replace the key with a personal access token to
read GitHub packages.

Find more
information [here](https://docs.github.com/en/enterprise-server@3.6/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
for setting up presonal access tokens.

## M series Macbook/Colima development setup

### Prerequisites

1. **Docker**: Ensure Docker is installed and running on your machine.
2. **Colima**: If you're using an M series Mac and Colima, you might need two Docker contexts/VMs:
   one for x86_64 architecture and another for arm64 architecture.

### Setting Up Virtual Machines (VMs)

#### Create VMs

We need two VMs, one for x86_64 and one for aarch64:

**VM Profile Overview Example:**

| PROFILE | STATUS  | ARCH    | CPUS | MEMORY | DISK  | RUNTIME | ADDRESS       |
|---------|---------|---------|------|--------|-------|---------|---------------|
| default | Running | x86_64  | 4    | 6GiB   | 50GiB | docker  | 192.168.106.2 |
| aarch64 | Running | aarch64 | 6    | 6GiB   | 50GiB | docker  | 192.168.106.3 |

**Create VMs using Colima:**

For aarch64:

```sh
colima start --cpu 6 --memory 6 --disk 50 --network-address --arch aarch64 --vm-type=vz --vz-rosetta --profile aarch64
```

For x86_64:

```sh
colima start --arch x86_64 --cpu 4 --memory 6 --disk 50 --network-address
```

#### Default Usage

The x86_64 profile will be used by default for running the Oracle DB. All other services should be
able to run on the aarch64 profile.

#### Restarting Profiles

If a profile fails to run, you can restart it using:

For aarch64:

```sh
colima start --profile aarch64
```

For default (x86_64):

```sh
colima start --profile default
```

### Switching Docker Contexts

To switch between Docker contexts, use the following commands:

Switch to default (x86_64) context:

```sh
unset DOCKER_HOST
docker context use colima
```

Switch to aarch64 context:

```sh
unset DOCKER_HOST
docker context use colima-aarch64
```

To check the current Docker context, use:

```sh
docker context show
```

## Recreating frontend static resources (CCMS stylesheets)

> **_INFORMATION:_**  When cloning this project for the first time, you do not need
> to run gulp as the static resources should already be created within the project.
> This tool is just in case you wish to recreate them.

To provide a clean way of recreating the static resources, a gulp workflow has been implemented. This
helps automate the creation of frontend static resources when new versions of frontend toolkits
have been released, and to compile the projects own style sheets into a minified format for better
browser performance for the end user.

### Prerequisites

In order to download/recompile frontend static resources, you will need to have `npm` installed.
It will also be used to download the dev dependencies required to compile the various resources.

With `npm` installed on your machine, you will need to ensure you have `gulp` installed on your
system globally rather than at project level.

```sh
npm install -g gulp
```

Check this has installed by checking the version.

```shell
gulp --version
```

Next download the dev dependencies for this project.

```sh
# Whilst in the project directory
npm install
```

### Gulp tasks and recreating the frontend resources

Recreating the frontend resources can be done by just running gulp.

There is just one task defined in `gulpfile.js` called `default`. When that task runs,
it will re-compile all the `.scss` files within the project into minified stylesheets, which are 
then stored in `src/main/resources/static/ccms`.

To run the default task, you can just run gulp without any additional parameters:
```shell
# Whilst in the project directory
gulp
```

## Snyk code analysis (CI/CD)
This project publishes vulnerability scans to the [LAA Snyk Dashboard (Google SSO)](https://app.snyk.io/org/legal-aid-agency).

If you cannot see the LAA organisation when logged into the dashboard,
please ask your lead developer/architect to have you added.

Scans will be triggered in two ways:

- Main branch - on commit, a vulnerability scan will be run and published to both the Snyk
  server and GitHub Code Scanning. Vulnerabilites will not fail the build.
- Feature branches - on commit, a vulnerability scan will be run to identify any new
  vulnerabilites (compared to the main branch). If new vulnerabilites have been raised. A code
  scan will also run to identify known security issues within the source code. If any issues are
  found, the build will fail.

### Running Snyk locally
To run Snyk locally, you will need to [install the Snyk CLI](https://docs.snyk.io/snyk-cli/install-or-update-the-snyk-cli).

Once installed, you will be able to run the following commands:

```shell
snyk test
```
For open-source vulnerabilies and licence issues. See [`snyk test`](https://docs.snyk.io/snyk-cli/commands/test).

```shell
snyk code test
```
For Static Application Security Testing (SAST) - known security issues. See [`snyk code test`](https://docs.snyk.io/snyk-cli/commands/code-test).

A [JetBrains Plugin](https://plugins.jetbrains.com/plugin/10972-snyk-security) is also available to integrate with your IDE. In addition to
vulnerabilities, this plugin will also report code quality issues.

### Configuration (`.snyk`)

The [.snyk](.snyk) file is used to configure exclusions for scanning. If a vulnerability is not
deemed to be a threat, or will be dealt with later, it can be added here to stop the pipeline
failing. See [documentation](https://docs.snyk.io/manage-risk/policies/the-.snyk-file) for more details.

### False Positives

Snyk may report that new vulnerabilities have been introduced on a feature branch and fail the
pipeline, even if this is not the case. As newly identified vulnerabilities are always being
published, the report for the main branch may become outdated when a new vulnerability is published.

If you think this may be the case, simply re-run the `monitor` command against the `main` branch
to update the report on the Snyk server, then re-run your pipeline.

Please ensure this matches the command used by the [build-main](.github/workflows/build-main.yml)
workflow to maintain consistency.

```shell
snyk monitor --org=legal-aid-agency --all-projects --exclude=build --target-reference=main
```

You should then see the new vulnerability in the LAA Dashboard, otherwise it is a new
vulnerability introduced on the feature branch that needs to be resolved.

## View metrics

This project exposes Actuator endpoints, which are scraped by a Prometheus instance. For debugging,
you can run a Prometheus instance locally within Docker:

```shell
docker-compose --compatibility -p laa-ccms-caab-development up -d laa-ccms-caab-prometheus
```