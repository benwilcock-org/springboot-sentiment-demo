# Spring Boot Sentiment Analysis Demo

## Overview

This microservice demonstrates enterprise-grade sentiment analysis using modern Java and machine learning. Built with **Spring Boot 4.0.5** and **Java 21 LTS**, it showcases how to integrate Deep Java Library (DJL) with PyTorch for production ML workloads.

The service analyzes text sentiment using a pre-trained **DistilBERT** model, returning probability scores for positive and negative sentiment through a REST API.

## Key Features

### AI/ML Capabilities
- **Pre-trained DistilBERT Model**: State-of-the-art transformer model for sentiment classification
- **PyTorch Backend**: Powered by DJL 0.26 with PyTorch engine and graph executor optimization
- **CPU-Optimized**: Runs efficiently on standard CPU infrastructure without GPU requirements
- **Automatic Model Loading**: Model downloaded and cached automatically from DJL model zoo

### Spring Boot Integration
- **Production-Ready**: Built on Spring Boot 4.0.5 with Jakarta EE 11 baseline
- **RESTful API**: Clean REST endpoint with JSON input/output
- **OpenAPI 3.1 Documentation**: Auto-generated API docs with SpringDoc OpenAPI 3.0.2
- **Actuator Monitoring**: Health checks and metrics via Spring Boot Actuator
- **Micrometer Tracing**: Distributed tracing support with Brave

### Developer Experience
- **Java 21 LTS**: Modern Java with long-term support until September 2031
- **Maven Wrapper**: No Maven installation required - `./mvnw` handles everything
- **Hot Reload**: Spring Boot DevTools for rapid development
- **Backstage Integration**: Registered as a component with TechDocs, OpenAPI spec, and Jenkins CI

## Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| **Java** | 21 (LTS) | Runtime platform with virtual threads, pattern matching, records |
| **Spring Boot** | 4.0.5 | Application framework with Jakarta EE 11 |
| **Spring Framework** | 7.0.6 | Core dependency injection and web framework |
| **DJL (Deep Java Library)** | 0.26 | ML framework with PyTorch engine |
| **SpringDoc OpenAPI** | 3.0.2 | API documentation generation (OpenAPI 3.1) |
| **Apache Tomcat** | 11.0.20 | Embedded servlet container |
| **Maven** | 3.9.9 | Build tool (via wrapper) |

**Last Updated:** March 29, 2026

## Getting Started

### Prerequisites

- **Java 21 or later** (JDK 21 recommended for LTS support)
- **No Maven installation required** - Maven wrapper included

### Running the Application

Start the application with the Maven wrapper:

```bash
./mvnw spring-boot:run
```

The application will:
1. Start on port **8080**
2. Download the DistilBERT model (~250MB) on first run
3. Perform a startup test prediction
4. Expose the API at `http://localhost:8080/api/analyze`

**First startup** takes ~60 seconds due to model download. Subsequent startups take ~5 seconds.

### Testing the API

#### Using cURL

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"sentence": "I love Spring Boot and DJL!"}'
```

**Example Response:**

```json
{
  "sentence": "I love Spring Boot and DJL!",
  "positive_probability": "99.94739%",
  "negative_probability": "0.05261%"
}
```

#### Using the HTTP File

The project includes `local-api-tests.http` with pre-configured requests. Open in your IDE (VS Code, IntelliJ) and click "Send Request".

#### Running Tests

```bash
./mvnw test
```

This executes integration tests that verify:
- Spring Boot context loads successfully
- DJL model loads from zoo
- Sentiment prediction works correctly

## API Reference

### POST /api/analyze

Analyzes the sentiment of a provided sentence.

**Request:**

```json
{
  "sentence": "Your text here"
}
```

**Response:**

```json
{
  "sentence": "Your text here",
  "positive_probability": "percentage",
  "negative_probability": "percentage"
}
```

**Status Codes:**
- `200 OK` - Analysis successful
- `400 Bad Request` - Invalid input (missing sentence)
- `500 Internal Server Error` - Model loading or prediction failure

### OpenAPI Documentation

Interactive API documentation available at:
- **JSON Spec:** http://localhost:8080/v3/api-docs
- **Swagger UI:** (if configured) http://localhost:8080/swagger-ui.html

The OpenAPI spec is automatically generated during build and available in `openapi/openapi.json` for Backstage integration.

## Monitoring & Observability

### Health Checks

```bash
curl http://localhost:8080/actuator/health
```

Returns application health status including readiness and liveness probes.

### Metrics

Spring Boot Actuator exposes metrics at `/actuator` for monitoring:
- JVM metrics (memory, threads, GC)
- HTTP request metrics
- Tomcat metrics

### Tracing

Micrometer Tracing with Brave provides distributed tracing capabilities for production deployments.

## Building & Packaging

### Build JAR

```bash
./mvnw clean package
```

Creates executable JAR at `target/djl-demo-0.0.1-SNAPSHOT.jar`

### Run Packaged JAR

```bash
java -jar target/djl-demo-0.0.1-SNAPSHOT.jar
```

### Full Build with Tests & OpenAPI Generation

```bash
./mvnw clean verify
```

This runs:
1. Compilation
2. Unit tests
3. Integration tests (starts/stops Spring Boot app)
4. OpenAPI spec generation
5. JAR packaging

## Continuous Integration

This project uses **Jenkins** for automated builds:

- **Schedule:** Weekly builds every Monday at 8 AM
- **Trigger:** Manual builds via "Build Now" button (webhook currently disabled)
- **Pipeline:** Declarative pipeline in `Jenkinsfile`
- **Tools:** Maven 3, JDK 21
- **Steps:** Clean → Test → Publish Results → Archive Artifacts
- **Status:** 75+ successful builds with 100% recent stability

For complete Jenkins configuration, troubleshooting, and disaster recovery procedures, see the [Jenkins CI Configuration](jenkins.md) page.

## Architecture

### Request Flow

```
Client Request (POST /api/analyze)
    ↓
SentimentApiController (REST layer)
    ↓
SentimentService (interface)
    ↓
SentimentAnalyzer (implementation)
    ↓
DJL Criteria Builder (model loading)
    ↓
PyTorch DistilBERT Model (inference)
    ↓
PercentageFormatter (formatting)
    ↓
JSON Response
```

### Model Details

- **Architecture:** DistilBERT (distilled BERT)
- **Parameters:** ~66 million
- **Training:** Pre-trained on sentiment classification task
- **Input:** Text sentences (max ~512 tokens)
- **Output:** Binary classification (Positive/Negative) with probabilities
- **Engine:** PyTorch with CPU optimization
- **Storage:** Automatically cached in `~/.djl.ai/cache/`

### Performance Characteristics

- **Cold Start:** ~60 seconds (first model download)
- **Warm Start:** ~5 seconds (model cached)
- **Inference Time:** ~150-200ms per request (CPU)
- **Memory:** ~2GB heap recommended
- **Concurrency:** Multiple requests handled by Tomcat thread pool

## Troubleshooting

### Model Download Fails

**Symptom:** `ModelNotFoundException` on startup

**Solution:**
1. Check internet connectivity
2. Verify `~/.djl.ai/cache/` directory permissions
3. Clear cache and retry: `rm -rf ~/.djl.ai/cache/`

### Out of Memory Errors

**Symptom:** `OutOfMemoryError` during model loading

**Solution:**
Increase JVM heap size:
```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx4g"
```

### Slow Inference

**Symptom:** API responses taking >1 second

**Solution:**
- DJL 0.26 includes PyTorch graph executor optimization (enabled by default)
- Ensure model is cached (not re-downloading on every request)
- Monitor with: `curl http://localhost:8080/actuator/metrics/http.server.requests`

### Port Already in Use

**Symptom:** `Port 8080 was already in use`

**Solution:**
Change port in `application.yml` or use environment variable:
```bash
SERVER_PORT=8081 ./mvnw spring-boot:run
```

## About Deep Java Library (DJL)

Deep Java Library is an open-source, high-level, engine-agnostic Java framework for deep learning. DJL provides:

- **Native Java Experience:** Works like any regular Java library
- **Engine Agnostic:** Supports PyTorch, TensorFlow, MXNet, and more
- **Production Ready:** Used by AWS and other enterprises
- **Easy to Use:** Designed for Java developers without ML expertise

Learn more at [djl.ai](https://djl.ai)

## Contributing

This is a demonstration project for Backstage integration. For issues or enhancements:

1. Check existing issues in the repository
2. Follow standard Git workflow for contributions
3. Ensure tests pass before submitting PRs
4. Update documentation for any API changes

## License

See LICENSE file in the repository root.