const gulp = require("gulp");
const {parallel} = require("gulp");
const sass = require("gulp-sass")(require("sass"));
const plumber = require('gulp-plumber');
const autoprefixer
    = require('gulp-autoprefixer').default || require('gulp-autoprefixer');
const sassOptions = {
  loadPaths: [
    ".",
    "./node_modules",
    './node_modules/govuk-frontend/dist',
    './node_modules/@ministryofjustice/frontend'
  ],
  style: "compressed"
};

function compileCCMSStyleSheets(){
  return gulp.src('./src/main/resources/scss/ccms.scss')
  // Will highlight errors in your CSS file incase they are missed
  .pipe(plumber())
  .pipe(sass(sassOptions))
  .pipe(plumber.stop())
  .pipe(autoprefixer({cascade: false})) // Adds up to date vendor prefixes
  .pipe(gulp.dest('./src/main/resources/static/ccms/'));
}

function compileCCMSAssessmentStyleSheets(){
  return gulp.src('./src/main/resources/scss/assessment-get.scss')
  // Will highlight errors in your CSS file incase they are missed
  .pipe(plumber())
  .pipe(sass(sassOptions))
  .pipe(plumber.stop())
  .pipe(autoprefixer({cascade: false})) // Adds up to date vendor prefixes
  .pipe(gulp.dest('./src/main/resources/static/ccms/'));
}

function compileMOJStyleSheets() {
  return gulp.src('./src/main/resources/scss/moj.scss')
  .pipe(plumber())
  .pipe(sass(sassOptions))
  .pipe(plumber.stop())
  .pipe(autoprefixer({ cascade: false }))
  .pipe(gulp.dest('./src/main/resources/static/ccms/'));
}


function copyMojAssets() {
  console.log("Copying MoJ assets");

  // Disable encoding for binary files,
  // see https://github.com/gulpjs/gulp/issues/2797
  return gulp.src("./node_modules/@ministryofjustice/frontend/moj/assets/**/*",
      { encoding: false })
  .pipe(gulp.dest("./src/main/resources/static/assets/"));
}


function copyGovukAssets() {
  console.log("Copying GOV.UK assets last");

  // Disable encoding for binary files,
  // see https://github.com/gulpjs/gulp/issues/2797
  return gulp.src("./node_modules/govuk-frontend/dist/govuk/assets/**/*",
      { encoding: false })
  .pipe(gulp.dest("./src/main/resources/static/assets/"));
}

function copyMojJs() {
  return gulp.src([
    './node_modules/@ministryofjustice/frontend/moj/all.bundle.js',
    './node_modules/@ministryofjustice/frontend/moj/all.bundle.js.map'
  ])
  .pipe(gulp.dest('./src/main/resources/static/assets/moj/'));
}

function copyGovukJs() {
  return gulp.src([
    './node_modules/govuk-frontend/dist/govuk/all.bundle.js',
    './node_modules/govuk-frontend/dist/govuk/all.bundle.js.map'
  ])
  .pipe(gulp.dest('./src/main/resources/static/assets/govuk/'));
}

// As a default task, it should just run all other tasks, so defined as a series
// by each other tasks name.
gulp.task('default',
    parallel(compileCCMSStyleSheets, compileCCMSAssessmentStyleSheets,
      compileMOJStyleSheets, copyGovukAssets, copyMojAssets, copyMojJs,
      copyGovukJs));
