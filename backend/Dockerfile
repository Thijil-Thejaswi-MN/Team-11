# Multi-stage Dockerfile for Team-11 Insurance Workflow Backend
# Stage 1: Build JAR using Maven and Temurin JDK 17
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy Maven POM and wrapper
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Pre-fetch dependencies to optimize Docker layer caching
RUN ./mvnw dependency:go-offline -B || mvn dependency:go-offline -B || true

# Copy source code and build production artifact
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal, secure runtime container using Temurin JRE 17
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Team 11 - Software Engineering"
LABEL description="Insurance Workflow Backend Application Instance"

# Install curl for container health check probes
RUN apk add --no-cache curl tzdata \
    && addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy built fat JAR from builder stage
COPY --from=builder /build/target/*.jar /app/app.jar

# Run as non-root user for container security
USER appuser:appgroup

# Expose backend port
EXPOSE 8080

# Environment defaults
ENV SPRING_PROFILES_ACTIVE=dev \
    SERVER_PORT=8080 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Healthcheck probe using Spring Boot Actuator
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=5 \
    CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
