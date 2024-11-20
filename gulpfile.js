const gulp = require("gulp");
const {parallel} = require("gulp");
const rename = require("gulp-rename");
const sass = require("gulp-sass")(require("sass"));


function copyGOVUKStyleSheets() {
  return gulp
  .src('./node_modules/govuk-frontend/dist/govuk/govuk-frontend.min.css')
  .pipe(rename('all.css'))
  .pipe(gulp.dest('./src/main/resources/static/govuk-frontend/'), {overwrite: true} );
}

function copyGOVUKJavaScript() {
  return gulp
  .src('./node_modules/govuk-frontend/dist/govuk/govuk-frontend.min.js')
  .pipe(rename('all.js'))
  .pipe(gulp.dest('./src/main/resources/static/js/'), {overwrite: true} );
}

function copyGOVUKAssets(){
  return gulp
  .src('./node_modules/govuk-frontend/dist/govuk/assets/**/*')
  .pipe(gulp.dest('./src/main/resources/static/assets/'), {overwrite: true} );
}

function copyMOJStyleSheets() {
  // MoJ don't contain
  return gulp
  .src('./node_modules/@ministryofjustice/frontend/moj/all.scss')
  .pipe(sass({
    outputStyle: 'compressed' // Minify the CSS output
  }).on('error', sass.logError))
  .pipe(rename('moj-frontend.min.css'))
  .pipe(gulp.dest('./src/main/resources/static/moj-frontend/'), {overwrite: true} );
}

function copyMOJJavaScript() {
  // MoJ doesn't provide a minified version of their JS file via npm, and if you try
  //  and use `gulp-minify`, it errors so this file is not minified.
  return gulp
  .src('./node_modules/@ministryofjustice/frontend/moj/all.js')
  .pipe(rename('moj-frontend.js'))
  .pipe(gulp.dest('./src/main/resources/static/moj-frontend/javascript'), {overwrite: true} );
}

function copyMOJAssets(){
  return gulp
  .src('./node_modules/@ministryofjustice/frontend/moj/assets/**/*')
  .pipe(gulp.dest('./src/main/resources/static/assets/'), {overwrite: true} );
}

// GOV UK Copy Task called "copyGOVUKAssets"
gulp.task('copyGOVUKAssets',
    parallel(copyGOVUKStyleSheets, copyGOVUKJavaScript, copyGOVUKAssets));
// GOV UK Copy Task called "copyMOJUKAssets"
gulp.task('copyMOJUKAssets', parallel(copyMOJStyleSheets, copyMOJJavaScript, copyMOJAssets));

// As a default task, it should just run all other tasks, so defined as a series
//  by each other tasks name.
gulp.task('default', gulp.series('copyGOVUKAssets', 'copyMOJUKAssets'));
