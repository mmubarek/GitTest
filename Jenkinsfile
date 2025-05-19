// Jenkinsfile
pipeline {
    agent any // Or specify an agent with Docker, JDK 21, Maven

    tools {
        maven 'Maven_3_9_X' // CRITICAL: Your Jenkins Maven tool name for Maven 3.9.x
        jdk 'JDK_21'        // CRITICAL: Your Jenkins JDK tool name for JDK 21
    }

    environment {
        TOKEN_LIB_DIR = 'simple-token-validator'
        TOKEN_API_DIR = 'token-api-spring-boot'
        // Use your Docker Hub username or private registry path
        DOCKER_REGISTRY_USER = credentials('dockerhub-username-credential-id') // Jenkins credential for Docker Hub username
        DOCKER_IMAGE_NAME = "${DOCKER_REGISTRY_USER}/my-token-service" // Example for Docker Hub
        DOCKER_IMAGE_TAG = "jdk21-build-${BUILD_NUMBER}"
        TOKEN_SECRET_CREDENTIAL_ID = 'your-app-token-secret-text-credential-id' // Jenkins secret text credential
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // --- Build and Test Token Library ---
        stage('Build & Unit Test Library') {
            steps {
                dir(env.TOKEN_LIB_DIR) {
                    sh 'mvn -B clean install' // Installs to Jenkins agent's local .m2
                }
            }
            post {
                always {
                    junit testResults: "${env.TOKEN_LIB_DIR}/target/surefire-reports/*.xml", allowEmptyResults: true
                }
            }
        }

        stage('SAST (SpotBugs on Library)') {
            // ... (same as your original, ensure paths are correct)
            steps {
                dir(env.TOKEN_LIB_DIR) {
                    sh 'mvn -B spotbugs:check || true'
                }
            }
            post {
                always {
                    recordIssues(
                        tools: [spotBugs(pattern: "${env.TOKEN_LIB_DIR}/target/spotbugsXml.xml")],
                        qualityGates: [
                            [threshold: 1, type: 'TOTAL_HIGH', unstable: true],
                            [threshold: 1, type: 'TOTAL_ERROR', failing: true]
                        ]
                    )
                }
            }
        }

        stage('SCA (OWASP Dependency-Check on Library)') {
            // ... (same as your original, ensure paths are correct)
            steps {
                dir(env.TOKEN_LIB_DIR) {
                    sh 'mvn -B org.owasp:dependency-check-maven:check || true'
                }
            }
            post {
                always {
                    dependencyCheckPublisher(pattern: "${env.TOKEN_LIB_DIR}/target/dependency-check-report.xml")
                    archiveArtifacts artifacts: "${env.TOKEN_LIB_DIR}/target/dependency-check-report.html", allowEmptyArchive: true
                }
            }
        }

        // --- Build and Test Token API Wrapper ---
        stage('Build & Unit Test API') {
            steps {
                dir(env.TOKEN_API_DIR) {
                    sh 'mvn -B clean package' // Runs unit tests
                }
            }
            post {
                always {
                    junit testResults: "${env.TOKEN_API_DIR}/target/surefire-reports/*.xml", allowEmptyResults: true
                    archiveArtifacts artifacts: "${env.TOKEN_API_DIR}/target/*.jar", fingerprint: true, allowEmptyArchive: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    // env.TOKEN_API_DIR is 'token-api-spring-boot'
                    // env.DOCKER_IMAGE_NAME could be a simple name like 'my-token-service' now
                    // env.DOCKER_IMAGE_TAG could be 'jdk21-build-${BUILD_NUMBER}' or just 'latest' for simplicity if only one is kept

                    // Example: Define a simple local image name if not pushing
                    def localImageName = "my-local-token-service" // Or use env.DOCKER_IMAGE_NAME if defined without username
                    def localImageTag = "build-${BUILD_NUMBER}"
                    def latestTag = "latest"

                    def fullImageNameWithTag = "${localImageName}:${localImageTag}"
                    def fullImageNameLatest = "${localImageName}:${latestTag}"

                    // Build the image using the Dockerfile from token-api-spring-boot,
                    // with the Jenkins workspace root as the build context.
                    // The "-f ${env.TOKEN_API_DIR}/Dockerfile ." part is crucial.
                    docker.build(fullImageNameWithTag, "-f ${env.TOKEN_API_DIR}/Dockerfile .")

                    // Optionally, tag this build as 'latest' as well for easier reference
                    // if you only care about the most recent build for local use.
                    sh "docker tag ${fullImageNameWithTag} ${fullImageNameLatest}"

                    echo "Docker image built locally on agent: ${fullImageNameWithTag} and tagged as ${fullImageNameLatest}"

                    // NO docker.withRegistry or customImage.push() here
                }
            }
        }

        // --- (Optional) Basic Smoke Test against Dockerized App ---
        stage('Smoke Test Deployed API') {
            environment {
                TOKEN_SECRET_FOR_TEST = credentials(TOKEN_SECRET_CREDENTIAL_ID)
            }
            steps {
                script {
                    def containerName = "token-api-smoke-${BUILD_NUMBER}"
                    // Use a dynamic host port or ensure it's free
                    // For simplicity, hardcoding, but better to find an ephemeral port
                    def hostPort = 18080 // Example port

                    try {
                        sh "docker ps -q --filter name=${containerName} | xargs -r docker stop || true"
                        sh "docker ps -aq --filter name=${containerName} | xargs -r docker rm || true"

                        // Run the container using the image just pushed/built
                        sh """
                            docker run -d --name ${containerName} \
                            -p ${hostPort}:8080 \
                            -e TOKEN_SECRET_KEY_ENV='${TOKEN_SECRET_FOR_TEST}' \
                            -e TOKEN_EXPIRY_MINUTES_ENV='5' \
                            ${env.DOCKER_IMAGE_NAME}:${env.DOCKER_IMAGE_TAG}
                        """
                        echo "Waiting for container ${containerName} on port ${hostPort} to start..."
                        sh "sleep 25" // Basic wait, a proper health check loop is better

                        // Test generate endpoint (simple check for 200 and token presence)
                        sh """
                            curl -s -o /dev/null -w "%{http_code}" -X POST \
                            -H "Content-Type: application/json" \
                            -d '{"userId":"jenkinsSmokeUser"}' \
                            http://localhost:${hostPort}/api/token/generate | grep 200
                        """
                        echo "Smoke test for generate endpoint passed (HTTP 200)."

                        // Add more tests: e.g., validate a generated token, check /actuator/health

                    } catch (any) {
                        error "Smoke test failed: ${any.getMessage()}"
                    } finally {
                        echo "Cleaning up smoke test container ${containerName}"
                        sh "docker stop ${containerName} || true"
                        sh "docker rm ${containerName} || true"
                    }
                }
            }
        }
    } // End of stages

    post {
        always {
            echo 'Pipeline finished.'
            // cleanWs()
        }
        success {
            echo 'Pipeline Succeeded!'
        }
        failure {
            echo 'Pipeline Failed!'
        }
        unstable {
            echo 'Pipeline is Unstable.'
        }
    }
}