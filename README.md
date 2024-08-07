# laa-ccms-caab-ui

## How to run this application:

The laa-ccms-caab-ui requires multiple other microservices in order to run locally and function 
correctly. They are:

- [laa-ccms-caab-api](https://github.com/ministryofjustice/laa-ccms-caab-api)
- [laa-ccms-caab-ebs-api](https://github.com/ministryofjustice/laa-ccms-data-api)
- [laa-ccms-caab-soa-api](https://github.com/ministryofjustice/laa-ccms-soa-gateway-api)
- [laa-ccms-caab-saml-mock](https://github.com/ministryofjustice/laa-ccms-caab-saml-mock)
- [laa-ccms-caab-db](https://github.com/ministryofjustice/laa-ccms-provider-ui-database)

## Set up laa-ccms-caab-saml-mock

This step requires maven to be installed on your machine. You can use [homebrew](https://formulae.brew.sh/formula/maven) to install it.

```
brew install maven
```

Next steps:

```
cd ..

git clone git@github.com:ministryofjustice/laa-ccms-caab-saml-mock.git laa-ccms-caab-saml-mock
cd laa-ccms-caab-saml-mock

mvn -B package --file pom.xml

cp mujina-idp/target/laa-ccms-caab-saml-mock-1.0.0.jar laa-ccms-caab-saml-mock-1.0.0.jar

cd ../laa-ccms-caab

docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-saml-mock

```

### Run laa-ccms-caab-saml-mock standalone

```
docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-saml-mock
```

## Setup laa-ccms-caab-db

```
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

```
docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-db
```

## Wiremock standalone 

This is required due to a dependency on the ordinance survey api, instead of calling the real thing we call this.
The wiremock can handle and postcode request.

```
docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-wiremock
```

## ClamAV 

To facilitate local virus scan when uploading files in the UI, a ClamAV container can be started using
the following command:

```
docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-clam-av
```

## LocalStack (AWS)

LocalStack provides lightweight instances of AWS components, such as S3. When running locally, it is
recommended to install [LocalStack Desktop](https://docs.localstack.cloud/user-guide/tools/localstack-desktop/)
to monitor and manage components. [LocalStack AWS CLI (`awslocal`)](https://docs.localstack.cloud/user-guide/integrations/aws-cli/#localstack-aws-cli-awslocal)
can also be useful if more in-depth interactions are required.

**Note: persistence is a pro feature, so files in S3 will not endure on container shutdown.**

A configured LocalStack container can be started via docker compose:

```
docker-compose --compatibility -p laa-ccms-caab-development up -d --build laa-ccms-caab-localstack
```

An S3 bucket `laa-ccms-documents` will be created on startup if it does not already exist, via
[`/localstack/init-s3.sh`](localstack/init-s3.sh).

## OPA/OIA and connector

The OPA/OIA and connector services are not required for the ui to start up locally, but if you want to test / develop features for the integration then this is required.

This [connector guide](https://github.com/ministryofjustice/laa-ccms-connector/blob/main/documentation/gradle-docker-build.md) can be followed to get the connector/opa/oia up and running.

If you are running on an M series mac and using colima. You will most likely need 2 docker contexts/vms. 
One running x86_64 architecture and the other running arm64 architecture. 

See guide below [M series Macbook/Colima development setup](#m-series-macbookcolima-development-setup) for more information.



## secrets.gradle (required for gradle build)

create a secrets.gradle file in the root directory:

```
project.ext.gitPackageUser = "{your name}"
project.ext.gitPackageKey = "{your personal api token}"
```

Replace the username with your own name, and replace the key with a personal access token to 
read GitHub packages.

Find more information [here](https://docs.github.com/en/enterprise-server@3.6/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens) for setting up presonal access tokens.


## GDS templates

These have been committed to the repo, but if you need these updated you can update the [templates](./templates.sh) script and rerun it.
Update the version when necessary.

## M series Macbook/Colima development setup

### Prerequisites
1. **Docker**: Ensure Docker is installed and running on your machine.
2. **Colima**: If you're using an M series Mac and Colima, you might need two Docker contexts/VMs: one for x86_64 architecture and another for arm64 architecture.

### Setting Up Virtual Machines (VMs)

#### Create VMs

We need two VMs, one for x86_64 and one for aarch64:

**VM Profile Overview Example:**

| PROFILE  | STATUS  | ARCH    | CPUS | MEMORY | DISK  | RUNTIME | ADDRESS       |
|----------|---------|---------|------|--------|-------|---------|---------------|
| default  | Running | x86_64  | 4    | 6GiB   | 50GiB | docker  | 192.168.106.2 |
| aarch64  | Running | aarch64 | 6    | 6GiB   | 50GiB | docker  | 192.168.106.3 |

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
The x86_64 profile will be used by default for running the Oracle DB. All other services should be able to run on the aarch64 profile.

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