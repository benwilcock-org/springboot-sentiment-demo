# Operations Guide

Deployment, monitoring, and performance optimization for production environments.

## Deployment Options

### Standalone JAR

!!! success "Recommended for Simple Deployments"
    Easy to deploy, no container runtime required.

**Build:**
```bash
./mvnw clean package
```

**Deploy:**
```bash
# Copy JAR to server
scp target/djl-demo-0.0.1-SNAPSHOT.jar user@server:/opt/sentiment-api/

# Run as systemd service
sudo systemctl start sentiment-api
```

**Systemd Service File** (`/etc/systemd/system/sentiment-api.service`):
```ini
[Unit]
Description=Sentiment Analysis API
After=network.target

[Service]
Type=simple
User=sentiment
WorkingDirectory=/opt/sentiment-api
ExecStart=/usr/bin/java -Xmx4g -jar djl-demo-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### Docker Container

!!! info "Best for Container Orchestration"
    Portable, consistent, works with Kubernetes.

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/djl-demo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx4g", "-jar", "app.jar"]
```

**Build Image:**
```bash
docker build -t sentiment-api:latest .
```

**Run Container:**
```bash
docker run -d \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xmx4g" \
  --name sentiment-api \
  sentiment-api:latest
```

**Docker Compose:**
```yaml
version: '3.8'
services:
  sentiment-api:
    image: sentiment-api:latest
    ports:
      - "8080:8080"
    environment:
      - JAVA_OPTS=-Xmx4g
      - SERVER_PORT=8080
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### Kubernetes

!!! tip "Production-Grade Orchestration"
    Auto-scaling, self-healing, rolling updates.

**Deployment YAML:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sentiment-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: sentiment-api
  template:
    metadata:
      labels:
        app: sentiment-api
    spec:
      containers:
      - name: sentiment-api
        image: sentiment-api:latest
        ports:
        - containerPort: 8080
        env:
        - name: JAVA_OPTS
          value: "-Xmx4g"
        resources:
          requests:
            memory: "2Gi"
            cpu: "500m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: sentiment-api
spec:
  selector:
    app: sentiment-api
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

**Horizontal Pod Autoscaler:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: sentiment-api-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: sentiment-api
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

## Resource Requirements

### Minimum Requirements

| Resource | Minimum | Recommended | Notes |
|----------|---------|-------------|-------|
| **CPU** | 1 core | 2 cores | Model inference is CPU-bound |
| **Memory** | 2GB | 4GB | Model + JVM heap |
| **Disk** | 500MB | 2GB | Model cache (~250MB) + logs |
| **Network** | 100 Mbps | 1 Gbps | Model download on first run |

!!! warning "Model Download"
    First startup requires downloading ~250MB model. Ensure sufficient bandwidth and disk space.

### Memory Configuration

**JVM Heap Sizing:**
```bash
# Development (minimal)
java -Xms1g -Xmx2g -jar app.jar

# Production (recommended)
java -Xms2g -Xmx4g -jar app.jar

# High load (aggressive)
java -Xms4g -Xmx8g -jar app.jar
```

**Garbage Collection Tuning:**
```bash
# G1GC (default in Java 21, recommended)
java -XX:+UseG1GC -Xmx4g -jar app.jar

# ZGC (low latency, Java 21+)
java -XX:+UseZGC -Xmx4g -jar app.jar
```

## Monitoring

### Health Checks

**Liveness Probe:**
```bash
curl http://localhost:8080/actuator/health/liveness
```

Returns:
```json
{"status": "UP"}
```

**Readiness Probe:**
```bash
curl http://localhost:8080/actuator/health/readiness
```

Returns:
```json
{"status": "UP"}
```

!!! info "Probe Differences"
    - **Liveness:** Is the app running? (restart if DOWN)
    - **Readiness:** Is the app ready to serve traffic? (remove from load balancer if DOWN)

### Metrics

**Actuator Metrics Endpoint:**
```bash
curl http://localhost:8080/actuator/metrics
```

**Available Metrics:**
- `jvm.memory.used` - Heap memory usage
- `jvm.gc.pause` - Garbage collection pauses
- `jvm.threads.live` - Active threads
- `http.server.requests` - HTTP request metrics
- `process.cpu.usage` - CPU utilization

**Specific Metric:**
```bash
curl http://localhost:8080/actuator/metrics/http.server.requests
```

### Prometheus Integration

**Add Dependency** (`pom.xml`):
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Enable Prometheus Endpoint** (`application.yml`):
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Scrape Endpoint:**
```bash
curl http://localhost:8080/actuator/prometheus
```

### Logging

**Log Levels** (`application.yml`):
```yaml
logging:
  level:
    root: INFO
    com.sentiment.djldemo: DEBUG
    ai.djl: INFO
    org.springframework.web: DEBUG
```

**Log to File:**
```yaml
logging:
  file:
    name: /var/log/sentiment-api/application.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

**JSON Logging** (for log aggregation):
```xml
<!-- Add logstash-logback-encoder dependency -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

## Performance Optimization

### Model Caching

DJL automatically caches the model at:
```
~/.djl.ai/cache/
```

!!! tip "Persistent Cache in Containers"
    Mount cache directory as volume to avoid re-downloading:
    ```bash
    docker run -v djl-cache:/root/.djl.ai/cache sentiment-api
    ```

### Response Time Optimization

**Cold Start Improvement:**
- Pre-download model during container build
- Use startup test to warm up model
- Persistent model cache volume

**Warm Request Optimization:**
- PyTorch graph executor enabled (DJL 0.26)
- CPU-optimized inference
- Connection pooling for concurrent requests

### Throughput Optimization

**Horizontal Scaling:**
```bash
# Kubernetes
kubectl scale deployment sentiment-api --replicas=5

# Docker Compose
docker-compose up --scale sentiment-api=3
```

**Load Balancing:**
- NGINX
- HAProxy  - Kubernetes Service (LoadBalancer)
- Cloud load balancers (AWS ALB, GCP LB)

### Resource Limits

**Container Limits:**
```yaml
resources:
  requests:
    memory: "2Gi"
    cpu: "500m"
  limits:
    memory: "4Gi"
    cpu: "2000m"
```

**Tomcat Tuning** (`application.yml`):
```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
    max-connections: 8192
    accept-count: 100
```

## High Availability

### Multi-Instance Deployment

!!! success "Stateless Design"
    Service is stateless - perfect for multi-instance deployment.

**Benefits:**
- No downtime during updates
- Automatic failover
- Load distribution
- Horizontal scaling

**Example Setup:**
```
                  ┌─────────────┐
Client ───────>   │Load Balancer│
                  └──────┬──────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
   ┌─────────┐      ┌─────────┐      ┌─────────┐
   │Instance1│      │Instance2│      │Instance3│
   └─────────┘      └─────────┘      └─────────┘
```

### Rolling Updates

**Kubernetes Rolling Update:**
```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

**Zero-Downtime Deployment:**
1. New instance starts
2. Readiness probe passes
3. Added to load balancer
4. Old instance removed
5. Repeat for all instances

### Circuit Breaker

**Resilience4j Integration:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

## Security

### HTTPS/TLS

**Configure SSL** (`application.yml`):
```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
```

**Generate Keystore:**
```bash
keytool -genkeypair -alias sentiment-api \
  -keyalg RSA -keysize 2048 \
  -storetype PKCS12 \
  -keystore keystore.p12 \
  -validity 365
```

### API Authentication

!!! warning "Not Implemented"
    Current version has no authentication. Add for production.

**Spring Security Example:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Rate Limiting

**Bucket4j Example:**
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.8.0</version>
</dependency>
```

## Backup & Recovery

### Model Cache Backup

!!! info "Model Persistence"
    Back up DJL cache to avoid re-downloading.

**Backup:**
```bash
tar -czf djl-cache-backup.tar.gz ~/.djl.ai/cache/
```

**Restore:**
```bash
tar -xzf djl-cache-backup.tar.gz -C ~/
```

### Configuration Backup

**Application Config:**
- `application.yml`
- `application-production.yml`
- Environment-specific properties

**Store in:**
- Git repository
- Configuration management (Consul, etcd)
- Kubernetes ConfigMaps/Secrets

## Disaster Recovery

### Recovery Time Objective (RTO)

**Target:** < 5 minutes

**Steps:**
1. Deploy from container registry: ~1 min
2. Model download (if not cached): ~2 min
3. Health checks pass: ~1 min

### Recovery Point Objective (RPO)

**Stateless Service:** RPO = 0 (no data loss)

### Runbook

**Service Outage Response:**

1. **Check health endpoint:**
   ```bash
   curl https://api.example.com/actuator/health
   ```

2. **Check logs:**
   ```bash
   kubectl logs deployment/sentiment-api --tail=100
   ```

3. **Restart service:**
   ```bash
   kubectl rollout restart deployment/sentiment-api
   ```

4. **Scale up if needed:**
   ```bash
   kubectl scale deployment sentiment-api --replicas=5
   ```

5. **Monitor recovery:**
   ```bash
   watch kubectl get pods
   ```

## Cost Optimization

### Right-Sizing Instances

!!! tip "Start Small, Scale Up"
    Begin with 2GB/1CPU, monitor, adjust based on load.

**Metrics to Monitor:**
- CPU utilization (target: 60-70%)
- Memory usage (target: 70-80%)
- Request latency (target: < 300ms p95)

### Auto-Scaling

**Scale based on:**
- CPU usage > 70%
- Memory usage > 75%
- Request queue depth
- Custom metrics (requests/second)

### Spot Instances

!!! warning "Use with Care"
    OK for batch processing, risky for real-time API.

**AWS Spot Instances:**
- 70-90% cost savings
- May be terminated with 2-minute notice
- Combine with on-demand instances

## Observability Stack

### Recommended Tools

**Metrics:**
- Prometheus (collection)
- Grafana (visualization)

**Logging:**
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Loki + Grafana

**Tracing:**
- Zipkin
- Jaeger
- AWS X-Ray

**Alerting:**
- Prometheus Alertmanager
- PagerDuty
- Opsgenie

### Sample Grafana Dashboard

**Key Panels:**
- Request rate (requests/second)
- Response time (p50, p95, p99)
- Error rate (%)
- JVM heap usage
- GC pause times
- CPU/Memory utilization

## Continuous Deployment

See [Jenkins CI Configuration](jenkins.md) for build pipeline details.

**Deployment Pipeline:**
1. Code commit → GitHub
2. Jenkins build triggered
3. Maven build + tests
4. Docker image built
5. Image pushed to registry
6. Kubernetes deployment updated
7. Rolling update applied
8. Health checks validated

## Next Steps

- **Troubleshoot issues:** [Troubleshooting](troubleshooting.md)
- **CI/CD setup:** [Jenkins CI Configuration](jenkins.md)
- **API usage:** [API Reference](api-reference.md)
