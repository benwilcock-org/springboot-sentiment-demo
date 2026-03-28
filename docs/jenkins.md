# Jenkins CI Configuration

This page documents the Jenkins continuous integration setup for the Spring Boot Sentiment Analysis Demo project.

## Overview

This project uses **Pipeline as Code** with a `Jenkinsfile` stored in the repository root. The pipeline runs on any available Jenkins agent and executes Maven-based builds with automated testing.

**Key Information:**
- **Pipeline location:** `/Jenkinsfile` in repository root
- **Pipeline type:** Declarative Pipeline (runs in sandbox mode)
- **Jenkins job:** `benwilcock-org/springboot-sentiment-demo`
- **Agent:** Uses any available agent (`agent any`)
- **Maven tool:** `maven-3` (configured in Jenkins Global Tool Configuration)
- **JDK:** Uses Jenkins default JDK (JDK 21 from jenkins/jenkins:lts-jdk21 image)

## Jenkinsfile Structure

The Jenkinsfile defines three main stages:

### Stage 1: Checkout
- Checks out code from SCM (GitHub)
- Logs the latest commit for debugging
- Verifies platform with `uname -a`

### Stage 2: Build and Test
- Executes `mvn clean test` to compile code and run unit tests
- Publishes JUnit test results from `target/surefire-reports/*.xml`
- Fails build if tests fail or no test results found

### Stage 3: Archive Artifacts
- Archives POM files as build artifacts
- Enables fingerprinting for artifact tracking
- Fails build if no artifacts found

### Post-Build Actions
- **Cleanup:** Removes `target/` and `.m2/repository/` to manage disk space
- **Logging:** Echoes build status (success/failure) with build number

## Build Triggers

The pipeline is triggered by:

1. **Scheduled Builds:** Weekly on Mondays around 8 AM (cron: `H 8 * * 1`)
2. **GitHub Webhook:** Automatic builds on push to `main` branch

## Tool Requirements

### Maven Configuration
The pipeline requires Maven to be configured in Jenkins:

| Tool Name | Type | Version | Configuration |
|-----------|------|---------|---------------|
| `maven-3` | Maven | 3.x | Configured in Global Tool Configuration |

**Important:** The tool name `maven-3` in the Jenkinsfile is **case-sensitive** and must match the Jenkins Global Tool Configuration exactly.

### JDK Configuration
The Jenkinsfile does **not** specify a JDK tool, which means:
- Jenkins uses its default JDK (JDK 21 from the jenkins/jenkins:lts-jdk21 image)
- No additional JDK configuration required in Global Tool Configuration
- The project compiles with Java 17 target (configured in pom.xml), which is compatible with JDK 21

### Platform Notes
The Jenkinsfile uses `agent any`, meaning:
- Build can run on any available Jenkins agent
- No specific platform labels required
- Jenkins will schedule the build on the first available agent

## Configuring Jenkins to Use the Jenkinsfile

This section provides step-by-step instructions for configuring a Jenkins server to use the repository-based Jenkinsfile. Use this when:
- Setting up a new Jenkins server
- Rebuilding Jenkins after server migration
- Creating a test job to validate pipeline changes

### Prerequisites

Before configuring the job, ensure these are set up in Jenkins:

#### 1. Tool Configuration

Navigate to **Manage Jenkins > Global Tool Configuration**:

**Maven Configuration:**
- Section: **Maven**
- Click **Add Maven**
- Name: `maven-3` (exact match required - case sensitive!)
- Install automatically: ✓ (recommended)
- Version: Latest Maven 3.x

**JDK Configuration:**
- No additional JDK configuration needed
- Jenkins uses its default JDK (JDK 21)
- The Jenkinsfile does not specify a JDK tool

#### 2. Agent Configuration

**No specific agent labels required:**
- The Jenkinsfile uses `agent any`
- Build will run on any available Jenkins agent
- No platform-specific labels needed

#### 3. GitHub Credentials

Ensure credentials exist for GitHub access:
- Navigate to **Manage Jenkins > Credentials**
- Add credentials if needed (Username/Password or SSH key)

### Job Configuration Steps

#### Step 1: Create or Navigate to Job

**For new job:**
1. Click **New Item**
2. Enter name: `benwilcock-org/springboot-sentiment-demo`
3. Select **Pipeline**
4. Click **OK**

**For existing job:**
1. Navigate to job dashboard
2. Click **Configure**

#### Step 2: General Settings

- **Description:** Spring Boot Sentiment Analysis Demo - Pipeline as Code
- **Discard old builds:** ✓
  - Days to keep builds: `90`
  - Max # of builds to keep: `30`

#### Step 3: Build Triggers (Optional)

GitHub webhook is recommended, but can configure fallback:
- **GitHub hook trigger for GITScm polling:** ✓ (if webhook configured)
- **Poll SCM:** Leave unchecked (webhook handles this)

**Note:** Cron schedule (`H 8 * * 1`) is defined in Jenkinsfile, not here.

#### Step 4: Pipeline Configuration

This is the **critical section** for using the Jenkinsfile:

**Definition:** `Pipeline script from SCM`

**SCM:** `Git`

**Repository Configuration:**
- **Repository URL:** `https://github.com/benwilcock-org/springboot-sentiment-demo.git`
- **Credentials:** Select GitHub credentials
- **Branch Specifier:** `*/main`

**Script Path:** `Jenkinsfile`

**Lightweight checkout:** ✓ (optional, improves performance)

#### Step 5: Save and Test

1. Click **Save**
2. Click **Build Now** to trigger first build
3. Watch build progress in **Console Output**

### Verification Checklist

After configuration, verify the following:

- [ ] Build starts and checks out code from GitHub
- [ ] Build logs show: `Running on` an available agent
- [ ] Build logs show: `using credential` for GitHub
- [ ] Build logs show: `Obtained Jenkinsfile from git`
- [ ] Maven tool `maven-3` resolves successfully
- [ ] `mvn clean test` executes
- [ ] JUnit test results appear in build summary
- [ ] POM artifacts archived successfully
- [ ] Workspace cleanup executes
- [ ] Build completes with "Finished: SUCCESS"

## GitHub Webhook Configuration

For automatic builds on push, configure GitHub webhook:

### On GitHub Repository

1. Navigate to repository: `https://github.com/benwilcock-org/springboot-sentiment-demo`
2. Go to **Settings > Webhooks > Add webhook**
3. **Payload URL:** `https://jenkins.wibbles.duckdns.org/github-webhook/`
4. **Content type:** `application/json`
5. **Events:** Just the push event
6. **Active:** ✓
7. Click **Add webhook**

### On Jenkins Server

1. Install **GitHub Plugin** (if not already installed)
2. Navigate to **Manage Jenkins > Configure System**
3. Section: **GitHub**
4. Add GitHub Server configuration if needed
5. Test connection

## Troubleshooting

### Build Fails: "Tool type 'maven' does not have an install of 'Maven 3'"

**Actual Error from Build #73:**
```
WorkflowScript: 11: Tool type "maven" does not have an install of "Maven 3" configured - did you mean "maven-3"?
```

**Cause:** Tool name in Jenkinsfile doesn't match Jenkins Global Tool Configuration

**Solution:**
1. Go to **Manage Jenkins > Global Tool Configuration > Maven**
2. Find the actual Maven tool name (e.g., `maven-3`)
3. Update Jenkinsfile to use the exact name:
   ```groovy
   tools {
       maven 'maven-3'  // Must match exactly (case-sensitive)
   }
   ```

### Build Fails: "Tool type 'jdk' does not have an install of 'JDK-17'"

**Actual Error from Build #73:**
```
WorkflowScript: 12: Tool type "jdk" does not have an install of "JDK-17" configured - did you mean "null"?
```

**Cause:** Jenkinsfile specified a JDK tool that doesn't exist in Jenkins configuration

**Solution:**
Remove the `jdk` line from Jenkinsfile tools section:
```groovy
tools {
    maven 'maven-3'
    // No jdk specification - uses Jenkins default JDK 21
}
```

### Build Stuck: "Waiting to schedule task" - Agent Label Not Found

**Actual Error from Build #74:**
```
Still waiting to schedule task
'Jenkins' doesn't have label 'linux&&aarch64'
```

**Cause:** Jenkinsfile requires agent labels that don't exist in Jenkins configuration

**Solution:**
Change the agent specification in Jenkinsfile to use any available agent:
```groovy
pipeline {
    agent any  // Use any available agent instead of specific labels
}
```

### Build Fails: Checkout Issues

**Cause:** GitHub credentials not configured or incorrect

**Solution:**
1. Verify credentials: **Manage Jenkins > Credentials**
2. Test GitHub connectivity from agent
3. Check Repository URL in job configuration is correct

### Build Slow: Large Maven Downloads

**Cause:** Maven downloads dependencies on first build or after workspace cleanup

**Solution:**
1. First build after migration will be slower (downloading dependencies)
2. Subsequent builds should be faster
3. Consider persistent Maven cache across builds if using dedicated agents
4. This is expected behavior - workspace cleanup removes `.m2/repository/` to save disk space

### Webhook Not Triggering Builds

**Cause:** GitHub webhook not configured or Jenkins not accessible

**Solution:**
1. Check webhook configuration on GitHub repository
2. Verify webhook delivery history shows successful deliveries
3. Check Jenkins is accessible from internet at webhook URL
4. Verify GitHub plugin installed and configured

## Build Configuration Details

### Build Retention Policy
- **Days to keep:** 90 days
- **Max builds:** 30 builds
- **Artifacts to keep:** Last 10 builds

### Timeout Policy
- **Max build duration:** 1 hour
- Prevents hung builds from consuming resources

### Concurrency Policy
- **Concurrent builds:** Disabled
- Only one build runs at a time for this job

### Workspace Cleanup
After every build (success or failure):
- Removes `target/` directory
- Removes `.m2/repository/` cache
- Keeps workspace otherwise intact

## Disaster Recovery

If Jenkins server needs to be rebuilt:

1. **Install Jenkins** on new server (e.g., jenkins/jenkins:lts-jdk21)
2. **Configure Maven tool** in Global Tool Configuration:
   - Navigate to **Manage Jenkins > Global Tool Configuration > Maven**
   - Add Maven with name: `maven-3` (exact match, case-sensitive)
3. **Set up GitHub credentials** in **Manage Jenkins > Credentials**
4. **Create job** following "Job Configuration Steps" above
5. **Configure webhook** on GitHub repository

All pipeline logic is in the `Jenkinsfile` in the repository - no inline scripts to recover.

## Build History

### Migration Timeline

- **Migration date:** 2026-03-28 (from inline pipeline to Jenkinsfile)
- **Builds before migration:** #1-72 (72+ builds with 100% stability using inline pipeline)
- **Migration builds:**
  - **Build #73 (FAILED):** Tool name mismatch - Jenkinsfile used `Maven 3` and `JDK-17` which didn't exist
  - **Build #74 (ABORTED):** Agent label issue - stuck waiting for `linux && aarch64` labels
  - **Build #75 (SUCCESS):** First successful Jenkinsfile build after fixing tool names and agent
- **Build history preserved:** Yes, build numbers continue incrementing

### Fixes Applied During Migration

1. Changed Maven tool from `Maven 3` to `maven-3` to match Jenkins configuration
2. Removed JDK tool specification (uses Jenkins default JDK 21)
3. Changed agent from `label 'linux && aarch64'` to `agent any`

## Related Documentation

- [CLAUDE.md](../CLAUDE.md) - Full project documentation including Jenkins integration section
- [catalog-info.yml](../catalog-info.yml) - Backstage integration (line 21: `jenkins.io/job-full-name`)
- [Jenkinsfile](../Jenkinsfile) - Pipeline definition source
- [pom.xml](../pom.xml) - Maven build configuration
