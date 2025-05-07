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
        
        // Jenkinsfile (SAST Stage)
        stage('SAST (SpotBugs)') {
            steps {
                dir(env.PROJECT_DIR) {
                    // Maven will now complete this step and generate the report,
                    // even if SpotBugs finds issues, due to failOnError=false in pom.xml.
                    // The '|| true' is less critical now but harmless.
                    sh 'mvn -B spotbugs:check || true'
                }
            }
            post {
                always {
                    recordIssues(
                        tools: [spotBugs(pattern: "${env.PROJECT_DIR}/target/spotbugsXml.xml")],
                        // Let Jenkins decide the build status based on the report:
                        qualityGates: [
                            // Example: 1 or more HIGH severity SpotBugs issues makes the build UNSTABLE
                            [threshold: 1, type: 'TOTAL_HIGH', unstable: true],
                            // Example: 1 or more ERROR severity SpotBugs issues FAILS the build
                            // (SpotBugs severities are often mapped to ERROR, HIGH, NORMAL, LOW by Warnings NG)
                            [threshold: 1, type: 'TOTAL_ERROR', failing: true], // Check how FindSecBugs severities are mapped in WarningsNG
                            // You might need to adjust 'type' based on how SpotBugs/FindSecBugs severities
                            // are categorized by the Warnings NG plugin (e.g., ERROR, WARNING_HIGH, etc.)
                            // Consult Warnings NG documentation or experiment.
                            // For FindSecurityBugs, its "high" issues are often treated as "ERROR" or "HIGH" by Warnings NG.
                        ]
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