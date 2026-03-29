# Architecture

Understanding the design and implementation of the sentiment analysis microservice.

## System Overview

The service follows a layered architecture with clean separation between REST, service, and ML layers.

```
┌─────────────────────────────────────────────┐
│           Client (REST API)                 │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│      SentimentApiController                 │
│      (REST Layer)                           │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│      SentimentService (Interface)           │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│      SentimentAnalyzer                      │
│      (Service Implementation)               │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│      DJL Criteria Builder                   │
│      (Model Loading)                        │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│      PyTorch DistilBERT Model               │
│      (ML Inference)                         │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│      PercentageFormatter                    │
│      (Response Formatting)                  │
└─────────────────────────────────────────────┘
```

## Request Flow

### Step-by-Step Execution

1. **HTTP Request Received**
   - Client sends POST to `/api/analyze`
   - Spring MVC routes to `SentimentApiController`
   - Request body deserialized to `Sentence` record

2. **Service Layer Processing**
   - Controller delegates to `SentimentService` interface
   - `SentimentAnalyzer` implementation executes

3. **Model Loading**
   - DJL Criteria builder creates model specification
   - Model loaded from cache or downloaded from zoo
   - PyTorch model initialized on CPU

4. **Inference**
   - Input sentence preprocessed and tokenized
   - DistilBERT model performs forward pass
   - Returns `Classifications` with probabilities

5. **Response Formatting**
   - `PercentageFormatter` converts probabilities to percentages
   - Response built with sentence and probabilities
   - JSON serialized and returned to client

### Performance Characteristics

| Phase | Cold Start | Warm Start |
|-------|-----------|------------|
| Model Loading | ~2-3 seconds | ~50ms (cached) |
| Inference | ~150ms | ~150ms |
| Formatting | <1ms | <1ms |
| **Total** | **~2-5 seconds** | **~150-200ms** |

## Components

### REST Layer

**File:** `SentimentApiController.java`

**Responsibilities:**
- HTTP request handling
- Input validation
- Response construction
- OpenAPI annotations

**Key Features:**
- `@RestController` for REST endpoint
- `@CrossOrigin` for CORS support
- DTOs using Java records (`Sentence`, `SentimentAnalysis`)
- Comprehensive OpenAPI documentation

### Service Layer

**Interface:** `SentimentService.java`

```java
public interface SentimentService {
    Optional<Classifications> predict(Optional<String> input)
        throws MalformedModelException, ModelNotFoundException,
               IOException, TranslateException;
}
```

**Implementation:** `SentimentAnalyzer.java`

**Design Pattern:** Interface-based dependency injection
- Enables testing with mocks
- Allows multiple implementations
- Follows SOLID principles

### ML Layer

**Deep Java Library (DJL) Integration:**

```java
Criteria<String, Classifications> criteria = Criteria.builder()
    .optApplication(Application.NLP.SENTIMENT_ANALYSIS)
    .setTypes(String.class, Classifications.class)
    .optDevice(Device.cpu())  // CPU-only execution
    .optProgress(new ProgressBar())
    .build();

try (ZooModel<String, Classifications> model = criteria.loadModel();
     Predictor<String, Classifications> predictor = model.newPredictor()) {
    return Optional.of(predictor.predict(input.get()));
}
```

**Resource Management:**
- Try-with-resources for automatic cleanup
- Model and predictor properly closed after use
- No resource leaks

### Formatting Layer

**File:** `PercentageFormatter.java`

**Purpose:** Convert probability values to human-readable percentages

**Format:** `##0.00000%` (5 decimal places)

**Example:**
- `0.998643` → `"99.86430%"`
- `0.001357` → `"0.13570%"`

## Data Flow

### Input Processing

**Request DTO (Record):**
```java
public record Sentence(
    @Schema(description = "The sentence to analyze")
    String sentence
) {}
```

**Validation:**
- Spring Boot validates `@Schema` constraints
- Empty sentences rejected at service layer

### Model Inference

**Model Specifications:**
- **Architecture:** DistilBERT (distilled BERT)
- **Parameters:** ~66 million
- **Input:** Tokenized text (max 512 tokens)
- **Output:** Binary classification scores

**Tokenization:**
- Automatic via DJL's translator
- Handles BERT-specific preprocessing
- Adds special tokens ([CLS], [SEP])

**Inference:**
- Forward pass through transformer layers
- Softmax activation for probabilities
- CPU execution (no GPU required)

### Response Construction

**Response DTO (Record):**
```java
public record SentimentAnalysis(
    String sentence,
    @JsonProperty("positive_probability") String positive,
    @JsonProperty("negative_probability") String negative
) {}
```

**JSON Serialization:**
- Automatic via Jackson
- Property naming via `@JsonProperty`
- Clean REST API contract

## Design Patterns

### Dependency Injection

**Constructor Injection:**
```java
@RestController
public class SentimentApiController {
    private final SentimentService sentimentService;
    private final PercentageService percentageService;

    public SentimentApiController(
        SentimentService sentimentService,
        PercentageService percentageService
    ) {
        this.sentimentService = sentimentService;
        this.percentageService = percentageService;
    }
}
```

**Benefits:**
- Testability with mocks
- Immutability (final fields)
- Clear dependencies

### Exception Handling

**Global Exception Handler:**

**File:** `SentimentApiControllerAdvice.java`

**Handles:**
- DJL exceptions → HTTP 500
- Model loading errors → HTTP 500
- Validation errors → HTTP 400

**Centralized Error Handling:**
- Consistent error responses
- Logging at failure points
- Clean separation of concerns

### Resource Management

**Try-with-Resources:**
- `ZooModel` implements `AutoCloseable`
- `Predictor` implements `AutoCloseable`
- Automatic resource cleanup

**Model Lifecycle:**
- Model loaded per request (not cached as singleton)
- DJL handles internal caching
- No memory leaks

## Model Details

### DistilBERT Overview

**What is DistilBERT?**
- Distilled version of BERT
- 40% smaller, 60% faster
- 97% of BERT's performance
- Optimized for production deployments

**Training:**
- Pre-trained on sentiment classification task
- Fine-tuned on labeled sentiment data
- Ready for zero-shot inference

### Model Storage

**Cache Location:**
```
~/.djl.ai/cache/
└── repo/
    └── model/
        └── nlp/
            └── sentiment_analysis/
                └── ai/djl/pytorch/
```

**Model Files:**
- PyTorch traced model (`.pt`)
- Tokenizer vocabulary
- Model metadata

**Size:** ~250MB

### PyTorch Engine

**DJL 0.26 Configuration:**
- PyTorch backend selected via `djl-spring-boot-starter-pytorch-auto`
- Graph executor optimizer enabled (new in 0.26)
- CPU-only execution (GPU not required)

**application.yml:**
```yaml
djl:
  application-type: SENTIMENT_ANALYSIS
```

## Spring Boot Integration

### Auto-Configuration

**DJL Spring Boot Starter:**
- Automatic DJL configuration
- Model zoo integration
- PyTorch engine setup
- No manual bean configuration needed

### Startup Behavior

**Context Initialization:**

**File:** `DjlDemoApplication.java`

```java
@EventListener(ContextRefreshedEvent.class)
public void testPrediction() {
    // Runs test prediction on startup
    // Warms up the model
    // Validates DJL integration
}
```

**Purpose:**
- Model pre-loading
- Integration validation
- Faster first API request

### Application Properties

**File:** `src/main/resources/application.yml`

```yaml
djl:
  application-type: SENTIMENT_ANALYSIS
  # Configures DJL to use sentiment analysis models

server:
  port: 8080
  # Can be overridden with SERVER_PORT env var
```

## Observability

### Logging

**SLF4J with Logback:**
- DEBUG: Model loading details
- INFO: Request processing, predictions
- WARN: Model issues
- ERROR: Exceptions

**Key Log Points:**
- Request received
- Sentiment analysis started
- Model loading (if needed)
- Prediction results
- Exception details

### Metrics

**Spring Boot Actuator:**
- JVM metrics (heap, GC, threads)
- HTTP metrics (requests, response times)
- Tomcat metrics (active connections)

**Micrometer Integration:**
- Brave tracing bridge
- Distributed tracing support
- Metrics export to monitoring systems

## Security Considerations

### Current State

**Not Production-Ready:**
- No authentication
- No authorization
- No input sanitization beyond validation
- CORS wide open (`*`)

### Production Recommendations

1. **Authentication:**
   - JWT tokens
   - OAuth2/OIDC
   - API keys

2. **Authorization:**
   - Role-based access control (RBAC)
   - Rate limiting per user/API key

3. **Input Validation:**
   - Max length enforcement
   - HTML escaping
   - SQL injection prevention (if DB added)

4. **CORS:**
   - Specific origins only
   - Credential support if needed

5. **HTTPS:**
   - TLS/SSL termination
   - Certificate management

## Scalability

### Horizontal Scaling

**Stateless Design:**
- No session state
- No in-memory caching (DJL handles it)
- Ready for load balancing

**Deployment Options:**
- Multiple instances behind load balancer
- Kubernetes with HPA (Horizontal Pod Autoscaler)
- Cloud platforms (AWS, Azure, GCP)

### Performance Optimization

**Current Optimizations:**
- PyTorch graph executor (DJL 0.26)
- Model caching by DJL
- CPU-optimized inference

**Future Improvements:**
- GPU support for higher throughput
- Model quantization for faster inference
- Batch processing for multiple sentences
- Caching frequent predictions

## Technology Decisions

### Why DJL?

- ✅ Native Java integration
- ✅ Engine-agnostic (PyTorch, TensorFlow, MXNet)
- ✅ Production-ready
- ✅ Active development (AWS-backed)
- ✅ Spring Boot starter available

### Why PyTorch?

- ✅ Most popular ML framework
- ✅ Best model ecosystem
- ✅ Strong community support
- ✅ Excellent Java bindings via DJL

### Why DistilBERT?

- ✅ Smaller than BERT (production-friendly)
- ✅ Faster inference
- ✅ High accuracy
- ✅ Pre-trained and ready to use
- ✅ CPU-friendly

### Why Spring Boot 4.0?

- ✅ Latest major version (longest support)
- ✅ Jakarta EE 11 baseline
- ✅ Spring Framework 7.0
- ✅ Modern Java 21 support
- ✅ Production-proven

## File Structure

```
src/main/java/com/sentiment/djldemo/
├── DjlDemoApplication.java          # Main class, startup test
├── SentimentApiController.java      # REST endpoint
├── SentimentApiControllerAdvice.java # Exception handler
├── SentimentService.java            # Service interface
├── SentimentAnalyzer.java           # Service implementation
├── PercentageService.java           # Formatting interface
└── PercentageFormatter.java         # Formatting implementation

src/main/resources/
└── application.yml                  # DJL configuration

src/test/java/com/sentiment/djldemo/
└── DjlDemoApplicationTests.java     # Integration test
```

## Next Steps

- **Deploy the service:** [Operations Guide](operations.md)
- **Troubleshoot issues:** [Troubleshooting](troubleshooting.md)
- **Understand the API:** [API Reference](api-reference.md)
