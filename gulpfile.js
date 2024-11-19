const gulp = require("gulp");
const {series, parallel} = require("gulp");
const rename = require("gulp-rename");
const sass = require("gulp-sass")(require("sass"));
const minify = require("gulp-minify");

function copyGOVUKStyleSheets() {
  return gulp
  .src('./node_modules/govuk-frontend/dist/govuk/govuk-frontend.min.css')
  .pipe(gulp.dest('./src/main/resources/static/css/'));
}

function copyGOVUKJavaScript() {
  return gulp
  .src('./node_modules/govuk-frontend/dist/govuk/govuk-frontend.min.js')
  .pipe(gulp.dest('./src/main/resources/static/js/'));
}

function copyMOJStyleSheets() {
  return gulp
  .src('./node_modules/@ministryofjustice/frontend/moj/all.scss')
  .pipe(sass().on('error', sass.logError))
  .pipe(rename('mojuk-frontend.css'))
  .pipe(gulp.dest('./src/main/resources/static/css/'));
}

function copyMOJJavaScript() {
  // MoJ doesn't provide a minified version of their JS file, and if you try
  //  and use `gulp-minify`, it errors so this file is not minified.
  return gulp
  .src('./node_modules/@ministryofjustice/frontend/moj/all.js')
  .pipe(rename('mojuk-frontend.js'))
  .pipe(gulp.dest('./src/main/resources/static/js/'));
}

// GOV UK Copy Task called "copyGOVUKAssets"
gulp.task('copyGOVUKAssets',
    parallel(copyGOVUKStyleSheets, copyGOVUKJavaScript));
// GOV UK Copy Task called "copyMOJUKAssets"
gulp.task('copyMOJUKAssets', parallel(copyMOJStyleSheets, copyMOJJavaScript));

// As a default task, it should just run all other tasks, so defined as a series
//  by each other tasks name.
gulp.task('default', gulp.series('copyGOVUKAssets', 'copyMOJUKAssets'));
