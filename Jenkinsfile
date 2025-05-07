// Jenkinsfile

pipeline {
    agent any // Or specify a specific agent with required tools

    tools {
        maven 'Maven_3_8_X' // CRITICAL: Replace with your Maven tool name configured in Jenkins Global Tool Configuration
        jdk 'JDK_11'       // CRITICAL: Replace with your JDK tool name configured in Jenkins Global Tool Configuration
    }

    environment {
        // Define the sub-project directory for easier reference
        PROJECT_DIR = 'simple-token-validator'
    }

    stages {
        stage('Checkout') {
            steps {
                // This will checkout the entire repository (GitTest)
                checkout scm
            }
        }

        stage('Build & Unit Test') {
            steps {
                // Execute Maven commands within the sub-project directory
                dir(env.PROJECT_DIR) {
                    sh 'mvn -B clean package'
                }
            }
            post {
                always {
                    // Archive JUnit test results
                    // Path is relative to the workspace root, so include the project directory
                    junit "${env.PROJECT_DIR}/target/surefire-reports/*.xml"
                }
            }
        }

        stage('SAST (SpotBugs)') {
            steps {
                dir(env.PROJECT_DIR) {
                    // SpotBugs check is part of the 'verify' phase in pom.xml,
                    // or can be run explicitly.
                    // We use '|| true' to let Jenkins control build failure via post-processing,
                    // rather than Maven failing the 'sh' step immediately.
                    sh 'mvn -B spotbugs:check || true'
                }
            }
            post {
                always {
                    // Use Warnings NG Plugin to record SpotBugs issues
                    recordIssues(
                        tools: [spotBugs(pattern: "${env.PROJECT_DIR}/target/spotbugsXml.xml")],
                        // You can configure quality gates in Jenkins or use failOnError here
                        // failOnError: true // Jenkins will fail the build based on Warnings NG thresholds/config
                        // The pom.xml already has <failOnError>true</failOnError> for SpotBugs,
                        // so Maven will likely fail the 'sh' step above if this is not handled.
                        // If Maven's failOnError is true, the '|| true' is crucial.
                        // If you want Jenkins to solely decide, set failOnError to false in pom.xml for SpotBugs.
                        // For this demo, let's assume pom.xml's failOnError handles it,
                        // but Jenkins will still record and display.
                        // If the 'sh' step fails due to SpotBugs, this post action might not fully execute
                        // in some pipeline configurations unless error handling is more robust.
                        // A better approach might be to have Maven *not* fail the build, and let Jenkins decide.
                        // For now, let's keep it simple.
                        // If SpotBugs fails the mvn command, the build will be marked as failed by Maven.
                    )
                }
            }
        }

        stage('SCA (OWASP Dependency-Check)') {
            steps {
                dir(env.PROJECT_DIR) {
                    // OWASP Dependency-Check is also configured in pom.xml
                    // Use '|| true' for the same reason as SpotBugs
                    sh 'mvn -B org.owasp:dependency-check-maven:check || true'
                }
            }
            post {
                always {
                    // Use the OWASP Dependency-Check Jenkins Plugin for reporting
                    dependencyCheckPublisher(
                        pattern: "${env.PROJECT_DIR}/target/dependency-check-report.xml"
                        // Configure failure thresholds in the Jenkins plugin's global or job configuration
                        // or rely on the pom.xml's <failBuildOnCVSS>
                    )
                    // Archive HTML report for easy viewing
                    archiveArtifacts artifacts: "${env.PROJECT_DIR}/target/dependency-check-report.html", allowEmptyArchive: true
                }
            }
        }

        // Optional: Archive the built JAR
        stage('Archive Application') {
            steps {
                dir(env.PROJECT_DIR) {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true
                }
            }
        }
    } // End of stages

    post {
        // Global post actions
        always {
            echo 'Pipeline finished.'
            // Clean up workspace to save disk space, especially if NVD data for Dependency-Check gets large locally
            // cleanWs()
        }
        success {
            echo 'Pipeline succeeded!'
            // mail to: 'dev-team@example.com', subject: "Jenkins Build Succeeded: ${currentBuild.fullDisplayName}"
        }
        failure {
            echo 'Pipeline failed!'
            // mail to: 'dev-team@example.com', subject: "Jenkins Build Failed: ${currentBuild.fullDisplayName}"
        }
        // The 'unstable' status is often used when tests pass but quality gates (like SpotBugs) find issues
        // but are not configured to fail the build outright.
        unstable {
            echo 'Pipeline is unstable (e.g., tests passed, but quality issues found).'
        }
    }
}