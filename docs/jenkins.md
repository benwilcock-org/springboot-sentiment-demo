# Jenkins CI Configuration

This page documents the Jenkins continuous integration setup for the Spring Boot Sentiment Analysis Demo project.

## Overview

This project uses **Pipeline as Code** with a `Jenkinsfile` stored in the repository root. The pipeline runs on Linux ARM64 (aarch64) architecture and executes Maven-based builds with automated testing.

**Key Information:**
- **Pipeline location:** `/Jenkinsfile` in repository root
- **Pipeline type:** Declarative Pipeline (runs in sandbox mode)
- **Jenkins job:** `benwilcock-org/springboot-sentiment-demo`
- **Build platform:** Linux ARM64 (aarch64) - required for PyTorch native libraries

## Jenkinsfile Structure

The Jenkinsfile defines four stages:

### Stage 1: Checkout
- Checks out code from SCM (GitHub)
- Logs the latest commit for debugging
- Verifies ARM64 platform with `uname -a`

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

## Platform Requirements

### ARM64 Architecture
This project **requires** Linux ARM64 (aarch64) architecture because:
- DJL downloads PyTorch native libraries during build
- PyTorch natives are platform-specific
- Using wrong architecture causes build failures

The Jenkinsfile enforces this with: `agent { label 'linux && aarch64' }`

### Tool Configuration
The pipeline requires two tools configured in Jenkins:

| Tool Name | Type | Version | Usage |
|-----------|------|---------|-------|
| `Maven 3` | Maven | 3.x | Build and test execution |
| `JDK-17` | JDK | 17 | Java compilation and runtime |

**Important:** Tool names in Jenkinsfile are **case-sensitive** and must match Jenkins configuration exactly.

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
- Name: `Maven 3` (exact match required)
- Install automatically: ✓ (recommended)
- Version: Latest Maven 3.x

**JDK Configuration:**
- Section: **JDK**
- Click **Add JDK**
- Name: `JDK-17` (exact match required)
- Install automatically: ✓ (recommended)
- Version: Java 17

#### 2. Agent Configuration

Ensure at least one agent has the correct labels:

Navigate to **Manage Jenkins > Nodes > [Agent Name] > Configure**:
- **Labels:** `linux aarch64` (space-separated)
- Verify agent is actually ARM64 architecture

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
- [ ] Build logs show: `Running on` an ARM64 agent
- [ ] Build logs show: `using credential` for GitHub
- [ ] Maven 3 tool resolves successfully
- [ ] JDK-17 tool resolves successfully
- [ ] `mvn clean test` executes
- [ ] JUnit test results appear in build summary
- [ ] POM artifacts archived successfully
- [ ] Workspace cleanup executes

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

### Build Fails: "No tool named Maven 3"

**Cause:** Tool name mismatch between Jenkinsfile and Jenkins configuration

**Solution:**
1. Go to **Manage Jenkins > Global Tool Configuration > Maven**
2. Verify tool name is exactly `Maven 3` (case-sensitive)
3. If different, either:
   - Rename Jenkins tool to match Jenkinsfile, OR
   - Update Jenkinsfile to match Jenkins tool name

### Build Fails: Platform Issues

**Cause:** Build running on wrong architecture (not ARM64)

**Solution:**
1. Check agent labels: **Manage Jenkins > Nodes > [Agent] > Configure**
2. Ensure labels include both `linux` AND `aarch64`
3. Verify agent is actually ARM64: SSH to agent and run `uname -m` (should output `aarch64`)

### Build Fails: Checkout Issues

**Cause:** GitHub credentials not configured or incorrect

**Solution:**
1. Verify credentials: **Manage Jenkins > Credentials**
2. Test GitHub connectivity from agent
3. Check Repository URL in job configuration is correct

### Build Slow: Large Downloads

**Cause:** DJL downloads PyTorch native libraries on every clean build

**Solution:**
1. Consider persistent Maven cache across builds
2. Reduce workspace cleanup aggressiveness in Jenkinsfile
3. This is expected behavior for ARM64 platform

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

1. **Install Jenkins** on new server
2. **Configure tools** (Maven 3, JDK-17) - see "Prerequisites" above
3. **Configure agent** with ARM64 labels
4. **Set up GitHub credentials**
5. **Create job** following "Job Configuration Steps" above
6. **Configure webhook** on GitHub repository

All pipeline logic is in the `Jenkinsfile` in the repository - no inline scripts to recover.

## Build History

- **Migration date:** 2026-03-28 (from inline pipeline to Jenkinsfile)
- **Builds before migration:** 72+ builds with 100% stability
- **Build history preserved:** Yes, build numbers continue incrementing

## Related Documentation

- [CLAUDE.md](../CLAUDE.md) - Full project documentation including Jenkins integration section
- [catalog-info.yml](../catalog-info.yml) - Backstage integration (line 21: `jenkins.io/job-full-name`)
- [Jenkinsfile](../Jenkinsfile) - Pipeline definition source
- [pom.xml](../pom.xml) - Maven build configuration
