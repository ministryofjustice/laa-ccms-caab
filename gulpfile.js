const gulp = require("gulp");
const {parallel, series} = require("gulp");
const rename = require("gulp-rename");
const uglify = require('gulp-uglify');
const sass = require("gulp-sass")(require("sass"));
const replace = require('gulp-replace');
const plumber = require('gulp-plumber');
const autoprefixer = require('gulp-autoprefixer').default || require('gulp-autoprefixer');

function copyGOVUKStyleSheets() {
  return gulp
  .src('./node_modules/govuk-frontend/dist/govuk/govuk-frontend.min.css')
  .pipe(rename('all.css'))
  // Add spring context path to asset locations
  .pipe(replace('/assets/', '/civil/assets/'))
  .pipe(gulp.dest('./src/main/resources/static/govuk-frontend/'), {overwrite: true} );
}

function copyGOVUKJavaScript() {
  return gulp
  .src('./node_modules/govuk-frontend/dist/govuk/govuk-frontend.min.js')
  .pipe(rename('all.js'))
  // Add spring context path to asset locations
  .pipe(replace('/assets/', '/civil/assets/'))
  .pipe(gulp.dest('./src/main/resources/static/js/'), {overwrite: true} );
}

function copyGOVUKAssets(){
  return gulp
  .src('./node_modules/govuk-frontend/dist/govuk/assets/**/*', { encoding: false })
  .pipe(gulp.dest('./src/main/resources/static/assets/'), {overwrite: true} );
}

function copyMOJStyleSheets() {
  // MoJ frontend does not come with a compiled minified css stylesheet, so
  //  compile it ourselves with 'sass'
  return gulp
  .src('./node_modules/@ministryofjustice/frontend/moj/all.scss')
  .pipe(sass({
    outputStyle: 'compressed' // Minify the CSS output
  }).on('error', sass.logError))
  .pipe(autoprefixer({cascade: false}))
  .pipe(rename('moj-frontend.min.css'))
  // Add spring context path to asset locations
  .pipe(replace('/assets/', '/civil/assets/'))
  .pipe(gulp.dest('./src/main/resources/static/moj-frontend/'), {overwrite: true} );
}

function copyMOJJavaScript() {
  // MoJ NPM package does not come with a 'minified' javascript, so uglify it
  //  ourselves. (Looking at the MoJ frontend repository, this is how they package
  //  the javascript themselves for GitHub release:
  //  https://github.com/ministryofjustice/moj-frontend/blob/main/gulp/dist.js)
  return gulp
  .src('./node_modules/@ministryofjustice/frontend/moj/all.js')
  .pipe(uglify())
  .pipe(rename('moj-frontend.min.js'))
  // Add spring context path to asset locations
  .pipe(replace('/assets/', '/civil/assets/'))
  .pipe(gulp.dest('./src/main/resources/static/moj-frontend/javascript'), {overwrite: true} );
}

function copyMOJAssets(){
  return gulp
  .src('./node_modules/@ministryofjustice/frontend/moj/assets/**/*', { encoding: false })
  .pipe(gulp.dest('./src/main/resources/static/assets/'), {overwrite: true} );
}

function compileCCMSStyleSheets(){
  return gulp.src('./src/main/resources/scss/ccms.scss')
  // Will highlight errors in your CSS file incase they are missed
  .pipe(plumber())
  .pipe(sass({
    includePaths: ['node_modules'],
    outputStyle: "compressed"
  }))
  .pipe(plumber.stop())
  .pipe(autoprefixer({cascade: false})) // Adds up to date vendor prefixes
  .pipe(gulp.dest('./src/main/resources/static/ccms/'));
}

function compileCCMSAssessmentStyleSheets(){
  return gulp.src('./src/main/resources/scss/assessment-get.scss')
  // Will highlight errors in your CSS file incase they are missed
  .pipe(plumber())
  .pipe(sass({
    outputStyle: "compressed"
  }))
  .pipe(plumber.stop())
  .pipe(autoprefixer({cascade: false})) // Adds up to date vendor prefixes
  .pipe(gulp.dest('./src/main/resources/static/ccms/'));
}

// GOV UK Copy Task called "copyGOVUKAssets"
gulp.task('copyGOVUKAssets',
    parallel(copyGOVUKStyleSheets, copyGOVUKJavaScript, copyGOVUKAssets));
// GOV UK Copy Task called "copyMOJUKAssets"
gulp.task('copyMOJAssets', parallel(copyMOJStyleSheets, copyMOJJavaScript, copyMOJAssets));

// CCMS Task called "compileCCMSAssets"
gulp.task('compileCCMSAssets', parallel(compileCCMSStyleSheets, compileCCMSAssessmentStyleSheets));

// As a default task, it should just run all other tasks, so defined as a series
//  by each other tasks name.
gulp.task('default', gulp.series('copyGOVUKAssets', 'copyMOJAssets', 'compileCCMSAssets'));
