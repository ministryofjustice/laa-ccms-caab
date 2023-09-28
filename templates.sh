# Remove existing GOV.UK Frontend assets
rm -rf src/main/resources/static/assets/fonts
rm -rf src/main/resources/static/assets/images
rm -rf src/main/resources/static/all*

# Get new release distribution assets and move to static directory
curl -L https://github.com/alphagov/govuk-frontend/releases/download/v4.6.0/release-v4.6.0.zip > govuk_frontend.zip
unzip -o govuk_frontend.zip -d src/main/resources/static
mkdir src/main/resources/static/govuk-frontend

mv src/main/resources/static/govuk-frontend-4.6.0.min.css src/main/resources/static/govuk-frontend/all.css
mv src/main/resources/static/govuk-frontend-4.6.0.min.js src/main/resources/static/govuk-frontend/all.js

# Tidy up
rm -rf src/main/resources/static/govuk-frontend-*
rm -rf govuk_frontend.zip
rm -rf src/main/resources/static/VERSION.txt

#TODO UPDATE FOR MOJ FRONTEND
curl -L https://github.com/ministryofjustice/moj-frontend/releases/download/v1.8.0/release-v1.8.0.zip > moj_frontend.zip
unzip -o moj_frontend.zip -d src/main/resources/static
mkdir src/main/resources/static/moj-frontend
mv src/main/resources/static/moj-frontend.min.css src/main/resources/static/moj-frontend/moj-frontend.min.css
rm -rf src/main/resources/static/moj-frontend-ie8.min.css
mv src/main/resources/static/moj-frontend.min.js src/main/resources/static/moj-frontend/javascript/moj-frontend.min.js
mv src/main/resources/static/assets/* src/main/resources/static/assets/

# Tidy up
rm -rf moj_frontend.zip


