const gulp = require("gulp");
const {parallel} = require("gulp");
const sass = require("gulp-sass")(require("sass"));
const plumber = require('gulp-plumber');
const autoprefixer = require('gulp-autoprefixer').default || require('gulp-autoprefixer');

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


// As a default task, it should just run all other tasks, so defined as a series
//  by each other tasks name.
gulp.task('default',
    parallel(compileCCMSStyleSheets, compileCCMSAssessmentStyleSheets));
