# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Gradle wrapper and build files first (for layer caching)
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle/libs.versions.toml gradle/

# Copy module build files
COPY core/build.gradle.kts core/
COPY api/build.gradle.kts api/
COPY connectors-sdk/build.gradle.kts connectors-sdk/
COPY connector-xwiki/build.gradle.kts connector-xwiki/
COPY connector-openproject/build.gradle.kts connector-openproject/

# Download dependencies (cached unless build files change)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code
COPY core/src/ core/src/
COPY api/src/ api/src/
COPY connectors-sdk/src/ connectors-sdk/src/
COPY connector-xwiki/src/ connector-xwiki/src/
COPY connector-openproject/src/ connector-openproject/src/

# Build the application
RUN ./gradlew :api:bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy the built jar
COPY --from=build /app/api/build/libs/opencrowd-api.jar app.jar

# Non-root user for security
RUN addgroup -S opencrowd && adduser -S opencrowd -G opencrowd
USER opencrowd

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
