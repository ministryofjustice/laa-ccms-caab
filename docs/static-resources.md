## Recreating frontend static resources (CCMS stylesheets)

> **_INFORMATION:_**  When cloning this project for the first time, you do not need
> to run gulp as the static resources should already be created within the project.
> This tool is just in case you wish to recreate them.

To provide a clean way of recreating the static resources, a gulp workflow has been implemented. This
helps automate the creation of frontend static resources when new versions of frontend toolkits
have been released, and to compile the projects own style sheets into a minified format for better
browser performance for the end user.

### Prerequisites

In order to download/recompile frontend static resources, you will need to have `npm` installed.
It will also be used to download the dev dependencies required to compile the various resources.

With `npm` installed on your machine, you will need to ensure you have `gulp` installed on your
system globally rather than at project level.

```sh
npm install -g gulp
```

Check this has installed by checking the version.

```shell
gulp --version
```

Next download the dev dependencies for this project.

```sh
# Whilst in the project directory
npm install
```

### Gulp tasks and recreating the frontend resources

Recreating the frontend resources can be done by just running gulp.

There is just one task defined in `gulpfile.js` called `default`. When that task runs,
it will re-compile all the `.scss` files within the project into minified stylesheets, which are
then stored in `src/main/resources/static/ccms`.

To run the default task, you can just run gulp without any additional parameters:
```shell
# Whilst in the project directory
gulp
```
