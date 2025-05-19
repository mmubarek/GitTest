# Stage 1: Build the application using Maven and JDK 21
# Base image should match your project's JDK target
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# --- Make simple-token-validator.jar available ---
# 1. COPY the pre-built simple-token-validator.jar into the build stage.
#    The source path 'simple-token-validator/target/simple-token-validator-1.0-SNAPSHOT.jar'
#    is relative to the Docker build context, which will be Git_Repository_Root.
COPY simple-token-validator/target/simple-token-validator-1.0-SNAPSHOT.jar /usr/src/app-libs/simple-token-validator.jar

# 2. Install the copied JAR to this build stage's local Maven repository.
#    This makes it resolvable by the subsequent Maven commands for token-api-spring-boot.
RUN mvn org.apache.maven.plugins:maven-install-plugin:3.1.1:install-file \
    -Dfile=/usr/src/app-libs/simple-token-validator.jar \
    -DgroupId=com.example \
    -DartifactId=simple-token-validator \
    -Dversion=1.0-SNAPSHOT \
    -Dpackaging=jar \
    -DgeneratePom=true \
    -DcreateChecksum=true
# --- End of making simple-token-validator.jar available ---

# Now, proceed with building token-api-spring-boot.
# COPY its pom.xml. The source path 'token-api-spring-boot/pom.xml'
# is relative to the Docker build context (Git_Repository_Root).
COPY token-api-spring-boot/pom.xml ./pom.xml
# Note: WORKDIR is /app, so this copies to /app/pom.xml

# Download dependencies for token-api-spring-boot.
# simple-token-validator will be found in the .m2 repo inside this container now.
RUN mvn validate dependency:go-offline -B
# COPY the source code for token-api-spring-boot.
# The source path 'token-api-spring-boot/src' is relative to the build context.
COPY token-api-spring-boot/src ./src
# Note: WORKDIR is /app, so this copies to /app/src

# Package the token-api-spring-boot application.
RUN mvn package -DskipTests

# Stage 2: Create the runtime image using JRE 21 slim
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV TOKEN_SECRET_KEY_ENV="override_this_secret_in_production_environment"
ENV TOKEN_EXPIRY_MINUTES_ENV="30"

EXPOSE 8080

# Copy the executable JAR for token-api-spring-boot from the 'build' stage.
# The path /app/target/*.jar is correct because it refers to the WORKDIR and 'target'
# directory *within the 'build' stage*.
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", \
            "-Dapp.token.secret-key=${TOKEN_SECRET_KEY_ENV}", \
            "-Dapp.token.expiry-minutes=${TOKEN_EXPIRY_MINUTES_ENV}", \
            "-jar", "app.jar"]