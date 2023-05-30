# Remove existing GOV.UK Frontend assets
rm -rf src/main/resources/static/assets/fonts
rm -rf src/main/resources/static/assets/images
rm -rf src/main/resources/static/govuk-frontend*

# Get new release distribution assets and move to static directory
curl -L https://github.com/alphagov/govuk-frontend/releases/download/v4.6.0/release-v4.6.0.zip > govuk_frontend.zip
unzip -o govuk_frontend.zip -d src/main/resources/static

# Tidy up
rm -rf govuk_frontend.zip