# Troubleshooting

Solutions to common issues and debugging techniques.

## Model Loading Issues

### Model Download Fails

!!! failure "Error: ModelNotFoundException"
    ```
    ai.djl.repository.zoo.ModelNotFoundException: No matching model with specified Input/Output type found
    ```

**Causes:**
- No internet connectivity
- Firewall blocking `mlrepo.djl.ai`
- Insufficient disk space
- Corrupted cache

**Solutions:**

1. **Check Internet Connectivity:**
   ```bash
   ping mlrepo.djl.ai
   curl -I https://mlrepo.djl.ai
   ```

2. **Check Disk Space:**
   ```bash
   df -h ~
   # Need ~500MB free for model download
   ```

3. **Clear DJL Cache:**
   ```bash
   rm -rf ~/.djl.ai/cache/
   ./mvnw spring-boot:run
   ```

4. **Configure Proxy** (if behind corporate firewall):
   ```bash
   export HTTPS_PROXY=http://proxy.example.com:8080
   ./mvnw spring-boot:run
   ```

5. **Manual Model Download:**
   ```bash
   # Download model manually and place in cache
   mkdir -p ~/.djl.ai/cache/repo/model/nlp/sentiment_analysis/ai/djl/pytorch/
   # Download from https://mlrepo.djl.ai/...
   ```

### Model Loading Timeout

!!! warning "Symptom: Application hangs on startup"

**Causes:**
- Slow network connection
- Large model download
- CPU throttling

**Solutions:**

1. **Increase Startup Timeout** (Kubernetes):
   ```yaml
   livenessProbe:
     initialDelaySeconds: 120  # Increase from 60
   ```

2. **Pre-download Model** (Docker):
   ```dockerfile
   # Add to Dockerfile
   RUN java -cp app.jar org.springframework.boot.loader.JarLauncher --download-model
   ```

3. **Use Persistent Cache Volume:**
   ```bash
   docker run -v djl-cache:/root/.djl.ai/cache sentiment-api
   ```

## Memory Issues

### OutOfMemoryError

!!! danger "Error: Java heap space"
    ```
    java.lang.OutOfMemoryError: Java heap space
    ```

**Causes:**
- Insufficient heap size
- Memory leak
- Too many concurrent requests

**Solutions:**

1. **Increase Heap Size:**
   ```bash
   # Development
   ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx4g"

   # Production JAR
   java -Xmx4g -jar app.jar

   # Docker
   docker run -e JAVA_OPTS="-Xmx4g" sentiment-api
   ```

2. **Check Current Memory Usage:**
   ```bash
   curl http://localhost:8080/actuator/metrics/jvm.memory.used | jq
   ```

3. **Monitor GC Activity:**
   ```bash
   java -XX:+PrintGCDetails -Xmx4g -jar app.jar
   ```

4. **Kubernetes Resource Limits:**
   ```yaml
   resources:
     requests:
       memory: "2Gi"
     limits:
       memory: "4Gi"
   ```

### Container OOMKilled

!!! failure "Kubernetes: Pod status OOMKilled"

**Cause:** Container exceeded memory limit

**Solutions:**

1. **Check Pod Events:**
   ```bash
   kubectl describe pod sentiment-api-xxx
   ```

2. **Increase Memory Limit:**
   ```yaml
   resources:
     limits:
       memory: "6Gi"  # Increased from 4Gi
   ```

3. **Reduce Heap to Leave Room for Native Memory:**
   ```yaml
   env:
   - name: JAVA_OPTS
     value: "-Xmx3g"  # Leave 1GB for native memory in 4GB container
   ```

## Performance Issues

### Slow Response Times

!!! warning "API responses > 1 second"

**Diagnostic Steps:**

1. **Check if Model is Cached:**
   ```bash
   ls -lh ~/.djl.ai/cache/
   # Should show ~250MB cached model
   ```

2. **Monitor Request Metrics:**
   ```bash
   curl http://localhost:8080/actuator/metrics/http.server.requests | jq
   ```

3. **Check CPU Usage:**
   ```bash
   top
   # Look for java process CPU %
   ```

4. **Enable Performance Logging:**
   ```yaml
   logging:
     level:
       com.sentiment.djldemo: DEBUG
       ai.djl: DEBUG
   ```

**Solutions:**

1. **Warm Up Model** (first request slow):
   - Startup test prediction handles this
   - If disabled, make dummy request after startup

2. **Increase CPU Allocation:**
   ```yaml
   resources:
     requests:
       cpu: "1000m"  # 1 full core
     limits:
       cpu: "2000m"  # 2 full cores
   ```

3. **Enable Graph Executor** (already enabled in DJL 0.26):
   - Check logs for: "PyTorch graph executor optimizer is enabled"

4. **Consider GPU** (for very high throughput):
   - Requires GPU-enabled instance
   - Change `Device.cpu()` to `Device.gpu()`
   - Much faster inference

### High CPU Usage

!!! warning "CPU consistently > 90%"

**Causes:**
- Too many concurrent requests
- Inefficient model execution
- Insufficient cores

**Solutions:**

1. **Horizontal Scaling:**
   ```bash
   kubectl scale deployment sentiment-api --replicas=5
   ```

2. **Increase CPU Limit:**
   ```yaml
   resources:
     limits:
       cpu: "4000m"  # 4 cores
   ```

3. **Add Load Balancer:**
   - Distribute load across instances
   - Implement request queuing

4. **Rate Limiting:**
   ```yaml
   # Add rate limiting (requires additional dependency)
   bucket4j:
     filters:
     - url: /api/.*
       rate-limits:
       - capacity: 100
         time: 1
         unit: minutes
   ```

## Application Startup Issues

### Port Already in Use

!!! failure "Error: Port 8080 was already in use"

**Solutions:**

1. **Kill Process Using Port:**
   ```bash
   # Find process
   lsof -i :8080

   # Kill it
   kill -9 <PID>
   ```

2. **Use Different Port:**
   ```bash
   SERVER_PORT=8081 ./mvnw spring-boot:run
   ```

3. **Configure in application.yml:**
   ```yaml
   server:
     port: 8081
   ```

### Context Initialization Failed

!!! danger "Error: ApplicationContextException"

**Causes:**
- Dependency injection failure
- Bean creation error
- DJL configuration issue

**Diagnostic Steps:**

1. **Check Full Stack Trace:**
   ```bash
   ./mvnw spring-boot:run 2>&1 | less
   ```

2. **Enable Debug Logging:**
   ```yaml
   logging:
     level:
       org.springframework: DEBUG
   ```

3. **Verify Dependencies:**
   ```bash
   ./mvnw dependency:tree
   ```

**Common Solutions:**

- Ensure Java 21+ is being used
- Verify all dependencies are downloaded
- Clear Maven cache: `rm -rf ~/.m2/repository/`

## API Request Issues

### 400 Bad Request

!!! failure "Empty or malformed request"

**Cause:** Missing `sentence` field

**Correct Request:**
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"sentence": "Your text here"}'
```

**Common Mistakes:**
```bash
# ❌ Missing sentence field
-d '{}'

# ❌ Wrong field name
-d '{"text": "..."}'

# ❌ Empty sentence
-d '{"sentence": ""}'

# ✅ Correct
-d '{"sentence": "Hello world"}'
```

### 500 Internal Server Error

!!! danger "Prediction failed"

**Check Logs:**
```bash
# Docker
docker logs sentiment-api

# Kubernetes
kubectl logs deployment/sentiment-api

# Local
tail -f logs/spring.log
```

**Common Causes:**

1. **Model Not Loaded:**
   - Check cache: `ls ~/.djl.ai/cache/`
   - Restart with clean cache

2. **Tokenization Failure:**
   - Check sentence length (max ~512 tokens)
   - Check for special characters

3. **PyTorch Error:**
   - Verify PyTorch dependency present
   - Check native library loading

### CORS Errors (Browser)

!!! failure "Cross-Origin Request Blocked"

**Browser Console Error:**
```
Access to XMLHttpRequest at 'http://localhost:8080/api/analyze'
from origin 'http://localhost:3000' has been blocked by CORS policy
```

**Cause:** CORS should be enabled, but check configuration

**Verify CORS** in `SentimentApiController.java`:
```java
@CrossOrigin  // Should allow all origins
@PostMapping("/analyze")
```

**Production CORS Configuration:**
```java
@CrossOrigin(origins = "https://yourdomain.com")
```

## Container/Kubernetes Issues

### Pod CrashLoopBackOff

!!! danger "Pod keeps restarting"

**Diagnostic Steps:**

1. **Check Pod Logs:**
   ```bash
   kubectl logs sentiment-api-xxx --previous
   ```

2. **Describe Pod:**
   ```bash
   kubectl describe pod sentiment-api-xxx
   ```

3. **Check Events:**
   ```bash
   kubectl get events --sort-by='.lastTimestamp'
   ```

**Common Causes:**
- OOMKilled (increase memory)
- Failed health checks (increase initialDelaySeconds)
- Missing model cache (add persistent volume)

### ImagePullBackOff

!!! failure "Cannot pull container image"

**Solutions:**

1. **Check Image Name:**
   ```bash
   kubectl describe pod sentiment-api-xxx | grep Image
   ```

2. **Verify Image Exists:**
   ```bash
   docker images | grep sentiment-api
   ```

3. **Check Registry Authentication:**
   ```bash
   kubectl create secret docker-registry regcred \
     --docker-server=<registry> \
     --docker-username=<username> \
     --docker-password=<password>
   ```

## Testing Issues

### Tests Failing

!!! failure "DjlDemoApplicationTests.contextLoads() fails"

**Diagnostic Steps:**

1. **Run Tests Verbose:**
   ```bash
   ./mvnw test -X
   ```

2. **Check Test Logs:**
   ```bash
   cat target/surefire-reports/*.txt
   ```

**Common Causes:**

1. **Model Download Timeout:**
   - Increase test timeout
   - Pre-download model before tests

2. **Memory Issues:**
   ```bash
   ./mvnw test -Dspring-boot.run.jvmArguments="-Xmx4g"
   ```

3. **Port Conflict:**
   - Another process using 8080
   - Random port in tests: `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)`

## Jenkins CI Issues

For Jenkins-specific issues, see [Jenkins CI Configuration](jenkins.md).

**Common Jenkins Problems:**
- Build timeout (increase timeout in Jenkinsfile)
- Maven cache issues (clean workspace)
- JDK version mismatch (verify JDK 21)

## Network Issues

### Cannot Connect to API

!!! failure "Connection refused"

**Diagnostic Steps:**

1. **Verify Application Running:**
   ```bash
   ps aux | grep java
   ```

2. **Check Port Binding:**
   ```bash
   netstat -an | grep 8080
   ```

3. **Test Localhost:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

4. **Check Firewall:**
   ```bash
   # Linux
   sudo iptables -L

   # macOS
   sudo /usr/libexec/ApplicationFirewall/socketfilterfw --listapps
   ```

**Solutions:**

1. **Bind to All Interfaces:**
   ```yaml
   server:
     address: 0.0.0.0  # Not just localhost
   ```

2. **Open Firewall Port:**
   ```bash
   # Linux
   sudo ufw allow 8080/tcp

   # macOS
   # Allow in System Preferences > Security & Privacy > Firewall
   ```

## Debug Mode

### Enable Detailed Logging

**application.yml:**
```yaml
logging:
  level:
    root: INFO
    com.sentiment.djldemo: DEBUG
    ai.djl: DEBUG
    org.springframework.web: DEBUG
    org.springframework.boot.web: DEBUG
```

**JVM Debug Mode:**
```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  -jar app.jar
```

Then connect debugger to port 5005.

### Enable Actuator Endpoints

**application.yml:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"  # Enable all endpoints (dev only!)
```

**Available Endpoints:**
```bash
curl http://localhost:8080/actuator

# Thread dump
curl http://localhost:8080/actuator/threaddump

# Heap dump
curl http://localhost:8080/actuator/heapdump -o heap.hprof

# Environment
curl http://localhost:8080/actuator/env

# Beans
curl http://localhost:8080/actuator/beans
```

## Getting Help

### Collect Diagnostic Information

Before asking for help, collect:

1. **Application Version:**
   ```bash
   curl http://localhost:8080/actuator/info
   ```

2. **Environment:**
   ```bash
   java -version
   ./mvnw --version
   uname -a
   ```

3. **Logs:**
   ```bash
   # Last 100 lines
   tail -100 application.log
   ```

4. **Configuration:**
   ```bash
   cat application.yml
   ```

5. **Dependency Tree:**
   ```bash
   ./mvnw dependency:tree > dependencies.txt
   ```

### Support Channels

- **GitHub Issues:** https://github.com/benwilcock-org/springboot-sentiment-demo/issues
- **DJL Community:** https://github.com/deepjavalibrary/djl/discussions
- **Spring Boot Docs:** https://spring.io/projects/spring-boot

## Next Steps

- **Optimize performance:** [Operations Guide](operations.md)
- **Understand architecture:** [Architecture](architecture.md)
- **API documentation:** [API Reference](api-reference.md)
