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

## GDS templates

These have been committed to the repo, but if you need these updated you can update the [templates](./templates.sh) script and rerun it.
Update the version when necessary.