# Makefile for LAA CCMS CAAB

# Define commands for Gradle
GRADLEW = ./gradlew

# TRIVY Configuration
# Exit with 1 if any vulnerabilities are found
TRIVY_SEVERITY = UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL
TRIVY_EXIT_CODE = 1
TRIVY_PKG_TYPES = os,library
TRIVY_IGNORE_UNFIXED = --ignore-unfixed

# Local image name for scanning
IMAGE_NAME = laa-ccms-caab

.PHONY: all
all: format audit build integration-test

.PHONY: format
format:
	@echo "🧹 Running spotlessApply..."
	$(GRADLEW) spotlessApply

.PHONY: audit
audit:
	@echo "🔍 Running Trivy audit for project dependencies..."
	@mkdir -p target
	@if command -v trivy >/dev/null 2>&1; then \
		trivy fs --severity $(TRIVY_SEVERITY) --exit-code $(TRIVY_EXIT_CODE) --pkg-types $(TRIVY_PKG_TYPES) $(TRIVY_IGNORE_UNFIXED) --ignorefile .trivyignore . > target/trivy-audit.log 2>&1; \
		status=$$?; \
		cat target/trivy-audit.log; \
		if [ $$status -ne 0 ]; then \
			echo "❌ Trivy audit failed — see target/trivy-audit.log"; \
			exit $$status; \
		fi; \
	else \
		echo "❌ Error: 'trivy' command not found. Please install it or ensure it is in your PATH."; \
		echo "👉 Visit https://trivy.dev/docs/latest/getting-started/installation/ for installation instructions."; \
		exit 127; \
	fi

.PHONY: test
test:
	@echo "🧪 Running unit tests..."
	$(GRADLEW) test

.PHONY: integration-test
integration-test:
	@echo "🧪 Running integration tests..."
	$(GRADLEW) integrationTest

.PHONY: build
build:
	@echo "🏗️ Building project..."
	$(GRADLEW) clean build

.PHONY: clean
clean:
	@echo "🧹 Cleaning project..."
	$(GRADLEW) clean
	rm -rf target

.PHONY: help
help:
	@echo "Available targets:"
	@echo "  all              - Run format, audit, test, integration-test, and build"
	@echo "  audit            - Run Trivy vulnerability scan on filesystem"
	@echo "  test             - Run unit tests"
	@echo "  integration-test - Run integration tests"
	@echo "  format           - Run spotlessApply to format code"
	@echo "  build            - Build the project (skipping tests)"
	@echo "  clean            - Clean build artifacts"
