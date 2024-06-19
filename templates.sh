# Remove existing GOV.UK Frontend assets
rm -rf src/main/resources/static/assets/fonts
rm -rf src/main/resources/static/assets/images
rm -rf src/main/resources/static/all*

# Get new release distribution assets and move to static directory
curl -L https://github.com/alphagov/govuk-frontend/releases/download/v5.3.1/release-v5.3.1.zip > govuk_frontend.zip
unzip -o govuk_frontend.zip -d src/main/resources/static
mkdir src/main/resources/static/govuk-frontend

mv src/main/resources/static/govuk-frontend-5.3.1.min.css src/main/resources/static/govuk-frontend/all.css
mv src/main/resources/static/govuk-frontend-5.3.1.min.js src/main/resources/static/govuk-frontend/all.js

# Update paths in CSS
sed -i '' 's|/assets/|/civil/assets/|g' src/main/resources/static/govuk-frontend/all.css

# Tidy up
rm -rf src/main/resources/static/govuk-frontend-*
rm -rf govuk_frontend.zip
rm -rf src/main/resources/static/VERSION.txt

curl -L https://github.com/ministryofjustice/moj-frontend/releases/download/v2.1.3/release-v2.1.3.zip > moj_frontend.zip
unzip -o moj_frontend.zip -d src/main/resources/static
mkdir src/main/resources/static/moj-frontend
mkdir src/main/resources/static/moj-frontend/javascript
mv src/main/resources/static/all.min.css src/main/resources/static/moj-frontend/moj-frontend.min.css
mv src/main/resources/static/moj-frontend.min.js src/main/resources/static/moj-frontend/javascript/moj-frontend.min.js
mv src/main/resources/static/assets/* src/main/resources/static/assets/

# Update paths in CSS
sed -i '' 's|/assets/|/civil/assets/|g' src/main/resources/static/moj-frontend/moj-frontend.min.css

# Tidy up
rm -rf moj_frontend.zip


