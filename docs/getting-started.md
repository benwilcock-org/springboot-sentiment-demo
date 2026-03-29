# Getting Started

This guide will help you get the sentiment analysis microservice up and running in minutes.

## Prerequisites

### Required
- **Java 21 or later** - JDK 21 LTS recommended for long-term support
  - Check version: `java -version`
  - Download: [Adoptium Eclipse Temurin](https://adoptium.net/)

### Optional
- **Maven** - Not required (Maven wrapper included)
- **IDE** - IntelliJ IDEA, VS Code, or Eclipse recommended
- **cURL** or **HTTPie** - For API testing

## Installation

### Clone the Repository

```bash
git clone https://github.com/benwilcock-org/springboot-sentiment-demo.git
cd springboot-sentiment-demo
```

### Verify Prerequisites

```bash
# Check Java version (must be 21+)
java -version

# Maven wrapper is included
./mvnw --version
```

Expected output:
```
Apache Maven 3.9.9
Java version: 21.0.x
```

## Running the Application

### Start the Service

```bash
./mvnw spring-boot:run
```

**First Startup (~60 seconds):**
- Maven downloads dependencies
- DJL downloads DistilBERT model (~250MB)
- Model is cached in `~/.djl.ai/cache/`
- Application performs startup test prediction

**Subsequent Startups (~5 seconds):**
- Model loaded from cache
- Much faster startup

### Verify Application Started

Look for this log message:
```
Started DjlDemoApplication in X.XXX seconds
```

The application is now running on **http://localhost:8080**

## Testing the API

### Using cURL

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"sentence": "Spring Boot is amazing!"}'
```

**Expected Response:**
```json
{
  "sentence": "Spring Boot is amazing!",
  "positive_probability": "99.87123%",
  "negative_probability": "0.12877%"
}
```

### Using HTTPie (Alternative)

```bash
http POST localhost:8080/api/analyze sentence="I love Java!"
```

### Using the Included HTTP File

The project includes `local-api-tests.http` with pre-configured requests:

1. Open `local-api-tests.http` in VS Code or IntelliJ
2. Click **"Send Request"** next to any request
3. View the response inline

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

## Running Tests

### Execute All Tests

```bash
./mvnw test
```

This runs:
- Integration test (context loads, DJL model loads)
- Application startup test prediction

**Expected Output:**
```
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Run Full Build

```bash
./mvnw clean verify
```

This executes:
1. Clean previous builds
2. Compile source code
3. Run tests
4. Start Spring Boot app (integration tests)
5. Generate OpenAPI spec
6. Stop Spring Boot app
7. Package JAR file

## Building for Deployment

### Create Executable JAR

```bash
./mvnw clean package
```

**Output:** `target/djl-demo-0.0.1-SNAPSHOT.jar`

### Run the Packaged JAR

```bash
java -jar target/djl-demo-0.0.1-SNAPSHOT.jar
```

### Run with Custom Configuration

```bash
# Use different port
java -jar target/djl-demo-0.0.1-SNAPSHOT.jar --server.port=8081

# Increase heap size for large models
java -Xmx4g -jar target/djl-demo-0.0.1-SNAPSHOT.jar
```

## Viewing API Documentation

### OpenAPI JSON Spec

```bash
curl http://localhost:8080/v3/api-docs | jq
```

Or visit: http://localhost:8080/v3/api-docs

### Generated OpenAPI File

After running `./mvnw verify`, the spec is available at:
```
openapi/openapi.json
```

This file is used by Backstage to display API documentation.

## Development Workflow

### Hot Reload with DevTools

Spring Boot DevTools enables automatic restart when code changes:

1. Make code changes
2. Rebuild the project (your IDE does this automatically)
3. Application restarts automatically

### IDE Setup

**IntelliJ IDEA:**
- Open `pom.xml` as a project
- Enable "Build project automatically" in Settings

**VS Code:**
- Install "Java Extension Pack"
- Install "Spring Boot Extension Pack"
- Open project folder

**Eclipse:**
- Import as "Existing Maven Project"
- Enable "Build Automatically"

## Configuration

### Application Properties

Edit `src/main/resources/application.yml`:

```yaml
djl:
  application-type: SENTIMENT_ANALYSIS

server:
  port: 8080

# Add custom configuration here
```

### Environment Variables

Override settings with environment variables:

```bash
# Change server port
SERVER_PORT=8081 ./mvnw spring-boot:run

# Set custom profile
SPRING_PROFILES_ACTIVE=production java -jar target/djl-demo-0.0.1-SNAPSHOT.jar
```

## What's Next?

- **Learn the API:** [API Reference](api-reference.md)
- **Understand the design:** [Architecture](architecture.md)
- **Deploy to production:** [Operations Guide](operations.md)
- **Having issues?:** [Troubleshooting](troubleshooting.md)

## Common First-Time Issues

**Model download fails:**
- Check internet connectivity
- Verify firewall allows HTTPS to `mlrepo.djl.ai`

**Out of memory:**
- Increase heap: `./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx4g"`

**Port 8080 in use:**
- Use different port: `SERVER_PORT=8081 ./mvnw spring-boot:run`

For more issues, see [Troubleshooting](troubleshooting.md).
