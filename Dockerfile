# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar
USER spring

ENV SERVER_PORT=3001
ENV SERVER_SERVLET_CONTEXT_PATH=/
EXPOSE 3001

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q -T 5 --spider http://localhost:${SERVER_PORT}${SERVER_SERVLET_CONTEXT_PATH%/}/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
