# syntax=docker/dockerfile:1

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
# The cache mount is a BuildKit feature (default in current Docker); the layer still works without it.
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar
USER spring

ENV SERVER_PORT=3001
ENV SERVER_SERVLET_CONTEXT_PATH=/
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 3001

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q -T 5 --spider http://localhost:${SERVER_PORT}${SERVER_SERVLET_CONTEXT_PATH%/}/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
