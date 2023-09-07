# laa-ccms-caab-ui

## How to run this application:

The laa-ccms-caab-ui requires multiple other microservcies in order to run locally and  function 
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

## secrets.gradle (required for gradle build)

create a secrets.gradle file in the root directory:

```
project.ext.gitPackageUser = "{your name}"
project.ext.gitPackageKey = "{your personal api token}"
```

Replace the username with your own name, and replace the key with a personal access token to read Github packages.

Find more information [here](https://docs.github.com/en/enterprise-server@3.6/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens) for setting up presonal access tokens.


## GDS templates

These have been committed to the repo, but if you need these updated you can update the [templates](./templates.sh) script and rerun it.
Update the version when necessary.