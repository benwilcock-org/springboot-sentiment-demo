# API Reference

Complete reference for the Sentiment Analysis API endpoints.

## Base URL

```
http://localhost:8080
```

## Authentication

Currently, no authentication is required. This is a demonstration service.

## Content Type

All requests and responses use **JSON**:
```
Content-Type: application/json
```

## Endpoints

### Analyze Sentiment

Analyzes the sentiment of a provided sentence and returns probability scores.

**Endpoint:** `POST /api/analyze`

**Request Body:**

```json
{
  "sentence": "string"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sentence` | string | Yes | Text to analyze (max ~512 tokens) |

**Response:** `200 OK`

```json
{
  "sentence": "string",
  "positive_probability": "string",
  "negative_probability": "string"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `sentence` | string | The original input sentence |
| `positive_probability` | string | Percentage probability of positive sentiment (formatted as "XX.XXXXX%") |
| `negative_probability` | string | Percentage probability of negative sentiment (formatted as "XX.XXXXX%") |

**Note:** Probabilities sum to ~100%

## Examples

### Positive Sentiment

**Request:**
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "sentence": "I absolutely love this product! It exceeded all my expectations."
  }'
```

**Response:**
```json
{
  "sentence": "I absolutely love this product! It exceeded all my expectations.",
  "positive_probability": "99.98321%",
  "negative_probability": "0.01679%"
}
```

### Negative Sentiment

**Request:**
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "sentence": "This is terrible and I hate it."
  }'
```

**Response:**
```json
{
  "sentence": "This is terrible and I hate it.",
  "positive_probability": "0.03421%",
  "negative_probability": "99.96579%"
}
```

### Neutral/Mixed Sentiment

**Request:**
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "sentence": "The product is okay, nothing special."
  }'
```

**Response:**
```json
{
  "sentence": "The product is okay, nothing special.",
  "positive_probability": "45.23456%",
  "negative_probability": "54.76544%"
}
```

## Error Responses

### 400 Bad Request

**Cause:** Missing or empty sentence field

**Request:**
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Response:**
```json
{
  "timestamp": "2026-03-29T10:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/analyze"
}
```

### 500 Internal Server Error

**Cause:** Model loading failure, prediction error, or internal exception

**Response:**
```json
{
  "timestamp": "2026-03-29T10:00:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to analyze sentiment",
  "path": "/api/analyze"
}
```

**Common Causes:**
- DJL model not downloaded
- Out of memory during inference
- Model file corrupted

See [Troubleshooting](troubleshooting.md) for solutions.

## Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| `200` | OK | Analysis successful |
| `400` | Bad Request | Invalid input (missing sentence) |
| `401` | Unauthorized | Not implemented (no auth required) |
| `403` | Forbidden | Not implemented (no auth required) |
| `404` | Not Found | Invalid endpoint path |
| `500` | Internal Server Error | Model or prediction failure |

## Rate Limiting

Currently, no rate limiting is implemented. This is a demonstration service.

For production deployments, consider adding:
- API Gateway with rate limiting
- Spring Cloud Gateway
- NGINX rate limiting

## OpenAPI Specification

### View OpenAPI JSON

**Endpoint:** `GET /v3/api-docs`

```bash
curl http://localhost:8080/v3/api-docs
```

Returns the complete OpenAPI 3.1 specification in JSON format.

### Generated OpenAPI File

After running `./mvnw verify`, the spec is available at:
```
openapi/openapi.json
```

This file is automatically registered in the Backstage catalog.

### OpenAPI Metadata

```json
{
  "openapi": "3.1.0",
  "info": {
    "title": "Sentiment Analysis API",
    "description": "An API for obtaining a sentiment analysis (percentage positive or negative) on a sentence that you provide.",
    "version": "0.1-SNAPSHOT"
  }
}
```

## CORS Configuration

The `/api/analyze` endpoint has **CORS enabled** with `@CrossOrigin`, allowing:
- All origins (`*`)
- All methods
- All headers

**Production Note:** Configure specific origins for production:
```java
@CrossOrigin(origins = "https://yourdomain.com")
```

## Request Validation

### Input Constraints

- **sentence**: Required, non-null, non-empty string
- **Max length**: ~512 tokens (DistilBERT limit)
- **Character encoding**: UTF-8

### Validation Behavior

**Empty sentence:**
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"sentence": ""}'
```

Returns `400 Bad Request` or `500 Internal Server Error` depending on validation layer.

## Performance

### Response Times

| Scenario | Typical Response Time |
|----------|----------------------|
| First request (cold start) | ~2-5 seconds |
| Subsequent requests (warm) | ~150-200ms |
| Concurrent requests | ~200-500ms |

**Factors affecting performance:**
- Model loaded in memory (first request slower)
- CPU cores available
- Sentence length
- Concurrent request load

### Throughput

- **Single instance:** ~5-10 requests/second (CPU-bound)
- **Horizontal scaling:** Linear improvement with additional instances
- **Resource usage:** ~2GB heap per instance

See [Operations Guide](operations.md) for performance optimization.

## Client Examples

### JavaScript/TypeScript (Fetch API)

```typescript
const response = await fetch('http://localhost:8080/api/analyze', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ sentence: 'I love this!' })
});

const result = await response.json();
console.log(result.positive_probability);
```

### Python (requests)

```python
import requests

response = requests.post(
    'http://localhost:8080/api/analyze',
    json={'sentence': 'I love this!'}
)

data = response.json()
print(data['positive_probability'])
```

### Java (HttpClient)

```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/api/analyze"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(
        "{\"sentence\": \"I love this!\"}"
    ))
    .build();

HttpResponse<String> response = client.send(
    request,
    HttpResponse.BodyHandlers.ofString()
);
```

## API Versioning

Currently, the API is **unversioned** (v0.1-SNAPSHOT). For production:

- Consider URL versioning: `/v1/api/analyze`
- Or header versioning: `Accept: application/vnd.sentiment.v1+json`
- Document deprecation policy
- Provide migration guides for breaking changes

## Testing the API

### Using HTTPie

```bash
# Install HTTPie
pip install httpie

# Make request
http POST localhost:8080/api/analyze sentence="Amazing!"
```

### Using Postman

1. Create new POST request
2. URL: `http://localhost:8080/api/analyze`
3. Body → raw → JSON:
   ```json
   {
     "sentence": "Your text here"
   }
   ```
4. Send

### Using the HTTP File

Open `local-api-tests.http` in VS Code or IntelliJ and click "Send Request".

## Next Steps

- **Understand the implementation:** [Architecture](architecture.md)
- **Deploy to production:** [Operations Guide](operations.md)
- **Troubleshoot issues:** [Troubleshooting](troubleshooting.md)
