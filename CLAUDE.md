# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Primary Purpose**: Demonstration project showing how to integrate components with the Backstage Software Catalog, including:
- Component and API entity registration in the Backstage catalog
- TechDocs integration for component documentation
- API records with OpenAPI specifications
- Jenkins CI integration for continuous integration monitoring

**Implementation**: Spring Boot 3.5.7 sentiment analysis microservice using Deep Java Library (DJL) with PyTorch. Exposes REST API endpoint (`POST /api/analyze`) that accepts sentences and returns positive/negative sentiment probabilities using pre-trained DistilBERT model.

## Backstage Integration

This project demonstrates four key Backstage integration patterns:

### 1. Component Registration

**catalog-info.yml** defines a Backstage Component entity:
- **Kind**: `Component` named `springboot-djl-demo`
- **Metadata**: Rich description, tags (java, spring, web, tanzu), GitHub links
- **Annotations**:
  - `backstage.io/techdocs-ref: dir:.` - Enables TechDocs from root directory
  - `backstage.io/kubernetes-label-selector` - Kubernetes integration
  - `github.com/project-slug` - GitHub integration
- **Spec**: Defines lifecycle (experimental), owner, system, and `providesApis` reference

### 2. TechDocs Integration

Documentation served through Backstage TechDocs:
- **mkdocs.yml** - MkDocs configuration defining site structure
- **docs/index.md** - Main documentation content (features, getting started, API usage, troubleshooting)
- **Annotation**: `backstage.io/techdocs-ref: dir:.` tells Backstage to build docs from root
- Demonstrates how to provide component documentation in the Backstage catalog

### 3. API Records

**catalog-info.yml** also defines a Backstage API entity:
- **Kind**: `API` named `sentiments-api`
- **Type**: `openapi` specification
- **Definition**: References generated OpenAPI spec at `/openapi/openapi.json`
- **Connection**: Component declares `providesApis: [sentiments-api]`
- Demonstrates how to register REST APIs in the Backstage catalog with their OpenAPI definitions

### 4. Jenkins CI Integration

**catalog-info.yml** includes Jenkins annotation for CI/CD monitoring:
- **Annotation**: `jenkins.io/job-full-name: 'benwilcock-org/springboot-sentiment-demo'`
- Enables Backstage Jenkins plugin to display build status and history
- **Jenkins Server**: Local Jenkins instance running on Podman (jenkins/jenkins:lts-jdk21)
  - Local access: http://localhost:8567 (port 8080 mapped to 8567)
  - Reverse proxy: https://jenkins.wibbles.duckdns.org
  - Agent port: 50000
- Demonstrates continuous integration monitoring within the Backstage component view

**Build Configuration**:
- **Pipeline location**: `Jenkinsfile` in repository root (Pipeline as Code)
- **Pipeline type**: Declarative Pipeline
- **Agent**: Uses any available Jenkins agent (`agent any`)
- **Tools**: Maven tool `maven-3`, uses default Jenkins JDK (JDK 21)
- **Build steps**:
  1. Checkout from SCM
  2. Execute `mvn clean test` using Maven 3
  3. Publish JUnit test results
  4. Archive build artifacts (POM files)
  5. Clean workspace (removes target/ and .m2/repository/)
- **Build triggers**:
  - Scheduled: Weekly builds every Monday around 8 AM (cron: `H 8 * * 1`) ✅ Active
  - GitHub webhook: Configured (ID: 603227540) but not functional ⚠️
    - Webhook exists and properly configured
    - Currently blocked by router/firewall (webhook times out)
    - Requires router port forwarding to enable automatic builds on push
    - Workaround: Use "Build Now" button for manual builds
- **Build health**: 75+ builds executed with 100% recent stability
- **Pipeline mode**: Runs in sandbox mode for security
- **Migration date**: 2026-03-28
- **First successful build from Jenkinsfile**: Build #75 (2026-03-28)
- **Documentation**: See TechDocs Jenkins CI page for configuration details

**Note**: Part of the larger 'Polyglot Demo' system (see comment line 1 in catalog-info.yml).

## Architecture

### Request Flow Pipeline

Complete request flow spans multiple components:

1. **SentimentApiController** receives `POST /api/analyze` with JSON body
2. Delegates to **SentimentService** interface (implemented by **SentimentAnalyzer**)
3. **SentimentAnalyzer** uses DJL Criteria builder to load DistilBERT model from zoo
4. Model performs inference and returns **Classifications** with Positive/Negative probabilities
5. **PercentageFormatter** converts probabilities to formatted percentages (`##0.00000%`)
6. Response built with sentence and both probabilities returned to client

### DJL Integration Pattern

- **Auto-configuration**: DJL configured via `djl-spring-boot-starter-pytorch-auto` dependency
- **Model selection**: `application.yml` sets `djl.application-type: SENTIMENT_ANALYSIS`
- **Model lifecycle**: Model loaded on-demand per request (not singleton cached)
- **CPU-only execution**: Model traced on CPU, must run on CPU (see `SentimentAnalyzer.java:64`)
- **Resource management**: Try-with-resources pattern for `ZooModel` and `Predictor` lifecycle

### Service Layer Design

Interface-based dependency injection pattern:
- **SentimentService** interface → **SentimentAnalyzer** implementation
- **PercentageService** interface → **PercentageFormatter** implementation
- Constructor injection in `SentimentApiController`
- Enables testing with mocks, production with real implementations

### Error Handling Strategy

Centralized exception management via **SentimentApiControllerAdvice**:
- Maps DJL exceptions (`MalformedModelException`, `ModelNotFoundException`, `TranslateException`) to HTTP 500
- Maps `IllegalArgumentException` (empty sentence) to HTTP 500 with custom `ErrorDescription`
- OpenAPI documentation includes all error responses (400, 401, 403, 404, 500)

### Startup Behavior

**DjlDemoApplication** has `@EventListener` for `ContextRefreshedEvent` (lines 47-52):
- Runs test prediction on startup: `"I like DJL. DJL is the best DL framework!"`
- Warms up the model before accepting requests
- Validates DJL integration is working correctly

## Common Commands

```bash
# Development
./mvnw spring-boot:run                    # Run application (downloads model on first run)
./mvnw test                               # Run all tests
./mvnw clean package                      # Build JAR

# Testing
./mvnw -Dtest=TestName test              # Run specific test

# OpenAPI generation
./mvnw verify                             # Runs integration tests and generates openapi/openapi.json
```

## Key Configuration Points

### Backstage Integration
- **Component catalog**: `catalog-info.yml` registers both Component and API entities in Backstage
- **TechDocs**: `backstage.io/techdocs-ref: dir:.` annotation + `mkdocs.yml` enables documentation
- **API registration**: API entity references `openapi/openapi.json` for interactive API docs in Backstage
- **Jenkins CI**: `jenkins.io/job-full-name` annotation connects to Jenkins job for build monitoring
- **System membership**: Component belongs to `system:default/red-hat-developer-hub`

### Application Configuration
- **DJL model selection**: `application.yml` configures `djl.application-type: SENTIMENT_ANALYSIS`
- **Java 17 required**: Mockito agent setup in `pom.xml` requires `-XX:+EnableDynamicAgentLoading`
- **PyTorch engine only**: Only `pytorch-auto` dependency enabled (MXNet/TensorFlow commented out)
- **OpenAPI generation**: `springdoc-openapi-maven-plugin` generates spec at `/v3/api-docs` during `mvnw verify`
- **Tanzu Platform**: `workload.yml` defines Cartographer workload with auto-scaling (min=1)
- **Startup test**: `ContextRefreshedEvent` listener runs test prediction to warm up model

## File Organization

```
# Backstage Integration Files (Primary Purpose)
catalog-info.yml                         # Backstage Component + API entity definitions
mkdocs.yml                               # MkDocs config for Backstage TechDocs
docs/index.md                            # TechDocs content (features, getting started)
openapi/openapi.json                     # Generated OpenAPI spec (referenced by API entity)

# Application Code
src/main/java/com/tanzu/djldemo/
  ├── DjlDemoApplication.java              # Entry point, OpenAPI config, startup test
  ├── SentimentApiController.java          # REST controller, DTOs, validation
  ├── SentimentApiControllerAdvice.java    # Global exception handlers
  ├── SentimentService.java                # Service interface
  ├── SentimentAnalyzer.java               # DJL model loading & inference
  ├── PercentageService.java               # Formatting interface
  └── PercentageFormatter.java             # Probability formatting

src/main/resources/
  └── application.yml                      # DJL configuration

# Build and Deployment
pom.xml                                  # Maven dependencies and plugins
workload.yml                             # Tanzu Application Platform deployment
local-api-tests.http                     # API test examples
```

## Important Patterns

- **Record-based DTOs**: `Sentence` and `SentimentAnalysis` use Java records
- **Optional-based APIs**: All service methods use `Optional<T>` for null safety
- **OpenAPI annotations**: Heavy use of `@Schema`, `@Operation`, `@ApiResponses` for API docs
- **Fixed percentage format**: `##0.00000%` defined in `PercentageService.PERCENTAGE_FORMAT`
- **CORS enabled**: `@CrossOrigin` on analyze endpoint for frontend integration
- **Logging**: DEBUG/INFO logging at service boundaries for observability
