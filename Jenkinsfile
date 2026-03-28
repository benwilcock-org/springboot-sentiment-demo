// Jenkinsfile for Spring Boot Sentiment Analysis Demo
// Pipeline as Code migration from inline Jenkins configuration
// Platform: Linux ARM64 (aarch64)

pipeline {
    agent any

    tools {
        maven 'maven-3'  // Must match Jenkins tool name exactly
    }

    options {
        buildDiscarder(logRotator(
            numToKeepStr: '30',
            daysToKeepStr: '90',
            artifactNumToKeepStr: '10'
        ))
        timestamps()
        timeout(time: 1, unit: 'HOURS')
        disableConcurrentBuilds()
    }

    triggers {
        // Weekly build every Monday around 8 AM
        cron('H 8 * * 1')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    sh 'git log -1 --oneline'
                    sh 'uname -a'  // Verify ARM64 platform
                }
            }
        }

        stage('Build and Test') {
            steps {
                sh 'mvn clean test'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml',
                          allowEmptyResults: false
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: '**/pom.xml',
                                 fingerprint: true,
                                 allowEmptyArchive: false
            }
        }
    }

    post {
        always {
            cleanWs(
                deleteDirs: true,
                patterns: [
                    [pattern: 'target/**', type: 'INCLUDE'],
                    [pattern: '.m2/repository/**', type: 'INCLUDE']
                ]
            )
        }
        success {
            echo "Build #${BUILD_NUMBER} completed successfully"
        }
        failure {
            echo "Build #${BUILD_NUMBER} failed"
        }
    }
}
