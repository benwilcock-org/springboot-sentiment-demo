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

For automatic builds on push, configure GitHub webhook. The webhook notifies Jenkins when code is pushed to the `main` branch.

### Method 1: Using GitHub Web UI

1. Navigate to repository: `https://github.com/benwilcock-org/springboot-sentiment-demo`
2. Go to **Settings > Webhooks > Add webhook**
3. **Payload URL:** `https://jenkins.wibbles.duckdns.org/github-webhook/`
4. **Content type:** `application/json`
5. **Events:** Just the push event
6. **Active:** ✓
7. Click **Add webhook**

### Method 2: Using GitHub CLI (Recommended for Automation)

For disaster recovery or scripted setup, use the GitHub CLI:

```bash
gh api \
  --method POST \
  -H "Accept: application/vnd.github+json" \
  repos/benwilcock-org/springboot-sentiment-demo/hooks \
  -f name='web' \
  -f config[url]='https://jenkins.wibbles.duckdns.org/github-webhook/' \
  -f config[content_type]='json' \
  -F config[insecure_ssl]=0 \
  -f events[]='push' \
  -F active=true
```

**Webhook created:** 2026-03-28 (ID: 603227540)

### Verifying Webhook Exists

To check if webhook is configured:

```bash
# List all webhooks for the repository
gh api repos/benwilcock-org/springboot-sentiment-demo/hooks --jq '.[] | {id, active, url: .config.url, events}'
```

Expected output:
```json
{
  "id": 603227540,
  "active": true,
  "url": "https://jenkins.wibbles.duckdns.org/github-webhook/",
  "events": ["push"]
}
```

### Testing the Webhook

After creating the webhook, test it:

1. **Manual test via GitHub API:**
   ```bash
   gh api -X POST repos/benwilcock-org/springboot-sentiment-demo/hooks/603227540/test
   ```

2. **Real test via code push:**
   - Make any commit and push to `main` branch
   - Check Jenkins for automatic build trigger
   - Verify build starts within ~30 seconds of push

3. **Check webhook delivery history:**
   - Go to GitHub repo **Settings > Webhooks**
   - Click on the webhook
   - View **Recent Deliveries** tab
   - Verify responses are `200 OK`

### On Jenkins Server

The Jenkins job is already configured to accept GitHub webhooks:

1. **GitHub Plugin** is installed
2. Job has `GitHubPushTrigger` enabled (configured via Jenkinsfile)
3. No additional Jenkins configuration needed

**Important:** The webhook triggers builds automatically on push. The cron schedule (`H 8 * * 1`) provides a backup in case webhooks fail.

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

**Cause:** GitHub webhook not configured, deleted, or Jenkins not accessible

**Diagnosis:**
```bash
# Check if webhook exists
gh api repos/benwilcock-org/springboot-sentiment-demo/hooks --jq '.[] | {id, active, url: .config.url}'
```

If the command returns `[]` (empty array), no webhook exists.

**Solution:**
1. **Create webhook** using GitHub CLI (see "GitHub Webhook Configuration" section):
   ```bash
   gh api --method POST \
     repos/benwilcock-org/springboot-sentiment-demo/hooks \
     -f name='web' \
     -f config[url]='https://jenkins.wibbles.duckdns.org/github-webhook/' \
     -f config[content_type]='json' \
     -f events[]='push' \
     -F active=true
   ```

2. **Test webhook** with a commit or manual trigger:
   ```bash
   # Get webhook ID from previous command, then test
   gh api -X POST repos/benwilcock-org/springboot-sentiment-demo/hooks/603227540/test
   ```

3. **Verify Jenkins receives webhook:**
   - Push a test commit
   - Check Jenkins job starts within 30 seconds
   - Look for "Started by GitHub push" in build console

4. **Check webhook delivery history** on GitHub:
   - Go to repo **Settings > Webhooks > Recent Deliveries**
   - Verify HTTP 200 responses
   - If 404/500 errors, check Jenkins URL is accessible from internet

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

If Jenkins server needs to be rebuilt, follow these steps in order:

### Step 1: Install Jenkins
```bash
# Example: Running Jenkins in Docker/Podman
podman run -d -p 8080:8080 -p 50000:50000 \
  --name jenkins \
  -v jenkins_home:/var/jenkins_home \
  jenkins/jenkins:lts-jdk21
```

### Step 2: Configure Maven Tool
1. Navigate to **Manage Jenkins > Global Tool Configuration > Maven**
2. Click **Add Maven**
3. Name: `maven-3` (exact match, case-sensitive)
4. Install automatically: ✓
5. Save

### Step 3: Set Up GitHub Credentials
1. Navigate to **Manage Jenkins > Credentials**
2. Add GitHub Personal Access Token or SSH key
3. Note the credential ID for job configuration

### Step 4: Create Jenkins Job
Follow "Job Configuration Steps" section above to create the pipeline job.

### Step 5: Create GitHub Webhook
Use GitHub CLI for automated setup:

```bash
gh api --method POST \
  -H "Accept: application/vnd.github+json" \
  repos/benwilcock-org/springboot-sentiment-demo/hooks \
  -f name='web' \
  -f config[url]='https://jenkins.wibbles.duckdns.org/github-webhook/' \
  -f config[content_type]='json' \
  -F config[insecure_ssl]=0 \
  -f events[]='push' \
  -F active=true
```

### Step 6: Test Setup
1. Trigger manual build: "Build Now"
2. Verify build succeeds
3. Test webhook: Make a test commit and push
4. Verify automatic build triggers

**Recovery time estimate:** 30-45 minutes for complete setup and testing.

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

### Post-Migration Configuration

- **GitHub Webhook created:** 2026-03-28 (webhook ID: 603227540)
  - Enables automatic builds on push to `main` branch
  - Created using GitHub CLI for reproducibility

## Related Documentation

- [CLAUDE.md](../CLAUDE.md) - Full project documentation including Jenkins integration section
- [catalog-info.yml](../catalog-info.yml) - Backstage integration (line 21: `jenkins.io/job-full-name`)
- [Jenkinsfile](../Jenkinsfile) - Pipeline definition source
- [pom.xml](../pom.xml) - Maven build configuration
