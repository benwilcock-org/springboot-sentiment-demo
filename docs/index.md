# Spring Boot Sentiment Analysis Demo

## Overview

An enterprise-grade NLP microservice demonstrating modern Java ML integration. Built with **Spring Boot 4.0.5** and **Java 21 LTS**, this service provides sentiment analysis and zero-shot text classification using a pre-trained **DistilBERT** model via Deep Java Library (DJL) and PyTorch.

**Key Capabilities:**
- AI-powered sentiment classification (positive/negative)
- Zero-shot text classification for custom categories
- Unified model architecture (single download, dual capabilities)
- RESTful API with JSON input/output
- Production-ready Spring Boot microservice
- Automatic ML model management
- OpenAPI 3.1 documentation
- Backstage catalog integration

## Quick Start

```bash
# Run the application
./mvnw spring-boot:run

# Test the API
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"sentence": "I love Spring Boot!"}'
```

**Response:**
```json
{
  "sentence": "I love Spring Boot!",
  "positive_probability": "99.94739%",
  "negative_probability": "0.05261%"
}
```

## Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| **Java** | 21 (LTS) | Runtime platform (LTS until 2031) |
| **Spring Boot** | 4.0.5 | Application framework |
| **Spring Framework** | 7.0.6 | Core framework (Jakarta EE 11) |
| **DJL** | 0.26 | ML framework with PyTorch |
| **SpringDoc OpenAPI** | 3.0.2 | API documentation (OpenAPI 3.1) |
| **Tomcat** | 11.0.20 | Embedded servlet container |
| **Maven** | 3.9.9 | Build tool (via wrapper) |

**Last Updated:** March 29, 2026

## Documentation Guide

**Getting Started**
- [Getting Started](getting-started.md) - Installation, running, and basic usage

**Reference**
- [API Reference](api-reference.md) - Complete API documentation with examples
- [Architecture](architecture.md) - System design and implementation details

**Operations**
- [Operations Guide](operations.md) - Deployment, monitoring, and performance
- [Troubleshooting](troubleshooting.md) - Common issues and solutions
- [Jenkins CI Configuration](jenkins.md) - Continuous integration setup

## Features

### AI/ML Capabilities
- **Pre-trained DistilBERT Model** - State-of-the-art transformer for NLP tasks
- **Unified Model Architecture** - Single model powers both sentiment analysis and text classification (memory efficient)
- **Zero-Shot Classification** - Classify text into custom categories without training
- **PyTorch Backend** - DJL 0.26 with graph executor optimization
- **CPU-Optimized** - No GPU required
- **Automatic Model Loading** - Downloaded and cached from DJL model zoo

### Spring Boot Integration
- **RESTful API** - Clean, documented REST endpoints (`/api/analyze`, `/api/classify`)
- **OpenAPI Documentation** - Auto-generated API specs
- **Actuator Monitoring** - Health checks and metrics
- **Micrometer Tracing** - Distributed tracing support

### Developer Experience
- **Java 21 LTS** - Modern Java with long-term support
- **Maven Wrapper** - No Maven installation required
- **Hot Reload** - Spring Boot DevTools
- **Backstage Integration** - TechDocs, OpenAPI, Jenkins CI

## Project Purpose

This project serves as a **Backstage integration demonstration**, showcasing:
- Component registration in the software catalog
- TechDocs documentation integration (this site)
- API entity registration with OpenAPI specs
- Jenkins CI pipeline integration

It demonstrates how to build production-ready ML microservices with Java and integrate them into your developer portal.

## Quick Links

- **GitHub Repository:** [springboot-sentiment-demo](https://github.com/benwilcock-org/springboot-sentiment-demo)
- **API Documentation:** http://localhost:8080/v3/api-docs (when running)
- **Health Check:** http://localhost:8080/actuator/health (when running)

## Next Steps

- **New to the project?** Start with [Getting Started](getting-started.md)
- **Want to use the API?** See [API Reference](api-reference.md)
- **Need to deploy?** Check [Operations Guide](operations.md)
- **Having issues?** Visit [Troubleshooting](troubleshooting.md)

## About Deep Java Library (DJL)

Deep Java Library is an open-source, high-level, engine-agnostic Java framework for deep learning. DJL provides native Java experience and works like any regular Java library, making ML accessible to Java developers.

Learn more at [djl.ai](https://djl.ai)
