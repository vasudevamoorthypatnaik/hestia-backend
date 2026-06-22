# =============================================================================
# Multi-stage Dockerfile for Hestia Backend (Spring Boot)
# Builds ARM64 images via Docker buildx for deployment to AWS t4g instances.
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Build the application with Maven
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

# Git metadata passed from CI (since .git/ is in .dockerignore)
ARG GIT_COMMIT_ID=unknown
ARG GIT_COMMIT_ID_ABBREV=unknown
ARG GIT_BRANCH=unknown
ARG GIT_COMMIT_TIME=unknown
ARG GIT_BUILD_TIME=unknown
ARG GIT_COMMIT_MESSAGE=unknown

WORKDIR /app

# Copy parent POM and all module POMs first to cache dependencies
COPY pom.xml .
COPY hestia-shared/pom.xml hestia-shared/
COPY hestia-notification/pom.xml hestia-notification/
COPY hestia-user/pom.xml hestia-user/
COPY hestia-event/pom.xml hestia-event/
COPY hestia-invitation/pom.xml hestia-invitation/
COPY hestia-app/pom.xml hestia-app/

# Download dependencies (cached unless POMs change)
RUN mvn dependency:go-offline -B

# Copy all source code and build
COPY . .

# Write git.properties from build args (since .git/ is excluded from Docker context)
RUN mkdir -p hestia-app/src/main/resources && \
    printf 'git.branch=%s\ngit.commit.id.full=%s\ngit.commit.id.abbrev=%s\ngit.commit.time=%s\ngit.build.time=%s\ngit.commit.message.short=%s\n' \
    "$GIT_BRANCH" "$GIT_COMMIT_ID" "$GIT_COMMIT_ID_ABBREV" "$GIT_COMMIT_TIME" "$GIT_BUILD_TIME" "$GIT_COMMIT_MESSAGE" \
    > hestia-app/src/main/resources/git.properties

RUN mvn clean package -DskipTests -Dmaven.gitcommitid.skip=true -B

# ---------------------------------------------------------------------------
# Stage 2: Lightweight runtime image
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install curl for Docker HEALTHCHECK
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser -s /sbin/nologin appuser

# Copy the built JAR from the build stage
COPY --from=build /app/hestia-app/target/*.jar app.jar

# JVM tuning for small instances (t4g.micro = 1GB RAM)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Spring Boot configuration (overridden by environment variables at runtime)
ENV SPRING_PROFILES_ACTIVE=prod

USER appuser
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -sf http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
