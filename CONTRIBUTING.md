# Contributing to the laa-ccms-caab-ui

## Signing your commits

Please ensure your commits are signed. See [Verifying commit signatures](https://docs.github.com/en/authentication/managing-commit-signature-verification/about-commit-signature-verification).

## Pre-commit hooks

This repository uses a combination of security and formatting hooks:

1.  **Security Hooks:** We use MoJ devsecops hooks for security scanning. Please follow the setup steps [here](https://github.com/ministryofjustice/devsecops-hooks?tab=readme-ov-file#development-prerequisites).
2.  **Formatting Hooks:** We use **Spotless** to ensure code consistency. A Git pre-commit hook is automatically installed in your local `.git/hooks` directory the first time you run a build (e.g., via `./gradlew compileJava`).

This hook automatically runs formatting checks before every commit.

## Code Quality and Formatting

We enforce code standards using **Google Java Format** via the Spotless Gradle plugin.

### Manual Formatting
Before pushing your code, it is good practice to run the formatters manually. You can use either `make` or `gradle`:

```bash
# Using the Makefile
make format

# Using Gradle directly
./gradlew spotlessApply
```

## Rebuilding static resources

See [rebuilding static resources](./docs/static-resources.md).

## Using feature flags

See [feature flags](./docs/feature-flags.md).