# laa-ccms-caab

## Set up Saml Mock
```
cd into laa-ccms-caab

git clone git@github.com:ministryofjustice/laa-saml-mock.git

cd laa-saml-mock

edit settings in IDE to change deprecated api error to warning

import laa-saml-mock module using maven template
make sure you have your ide settings for the laa-saml-mock set to java 1.8

mvn clean package from within the IDE
```

## Run saml mock standalone

```
docker-compose up --build identity-provider
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