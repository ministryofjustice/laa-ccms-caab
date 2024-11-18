#!/bin/bash

# Load versions from template_versions file
source template_versions

# Check if both versions are provided
if [[ -z "$GOVUK_VERSION" || -z "$MOJ_VERSION" ]]; then
  echo "Error: Version numbers for GOV.UK Frontend or MOJ Frontend are missing."
  exit 1
fi

echo "Updating GOV.UK Frontend to version $GOVUK_VERSION and MOJ Frontend to version $MOJ_VERSION"

# Remove existing GOV.UK Frontend assets
rm -rf src/main/resources/static/assets/fonts
rm -rf src/main/resources/static/assets/images
rm -rf src/main/resources/static/all*

# Get new GOV.UK Frontend release assets and move to static directory
curl -L https://github.com/alphagov/govuk-frontend/releases/download/v5.3.1/release-v"$GOVUK_VERSION".zip > govuk_frontend.zip
unzip -o govuk_frontend.zip -d src/main/resources/static
mkdir src/main/resources/static/govuk-frontend

mv src/main/resources/static/govuk-frontend-"$GOVUK_VERSION".min.css src/main/resources/static/govuk-frontend/all.css
mv src/main/resources/static/govuk-frontend-"$GOVUK_VERSION".min.js src/main/resources/static/govuk-frontend/all.js

# Update paths in GOV.UK Frontend CSS
sed -i '' 's|/assets/|/civil/assets/|g' src/main/resources/static/govuk-frontend/all.css

# Tidy up GOV.UK Frontend assets
rm -rf src/main/resources/static/govuk-frontend-*
rm -rf govuk_frontend.zip
rm -rf src/main/resources/static/VERSION.txt

# Get new MOJ Frontend release assets and move to static directory
curl -L https://github.com/ministryofjustice/moj-frontend/releases/download/v"$MOJ_VERSION"/release-v"$MOJ_VERSION".zip > moj_frontend.zip
unzip -o moj_frontend.zip -d src/main/resources/static
mkdir src/main/resources/static/moj-frontend
mkdir src/main/resources/static/moj-frontend/javascript

mv src/main/resources/static/all.min.css src/main/resources/static/moj-frontend/moj-frontend.min.css
mv src/main/resources/static/moj-frontend.min.js src/main/resources/static/moj-frontend/javascript/moj-frontend.min.js
mv src/main/resources/static/assets/* src/main/resources/static/assets/

# Update paths in MOJ Frontend CSS
sed -i '' 's|/assets/|/civil/assets/|g' src/main/resources/static/moj-frontend/moj-frontend.min.css

# Tidy up MOJ Frontend assets
rm -rf moj_frontend.zip

echo "Update completed for GOV.UK Frontend v$GOVUK_VERSION and MOJ Frontend v$MOJ_VERSION"
