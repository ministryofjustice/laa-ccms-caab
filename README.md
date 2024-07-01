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

An S3 bucket `laa-ccms-bucket` will be created on startup if it does not already exist, via
[`/localstack/init-s3.sh`](localstack/init-s3.sh).

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