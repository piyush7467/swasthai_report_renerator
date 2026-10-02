# =========================================================================
# SwasthAI Report Generator - Production Dockerfile for Railway / Cloud
# Multi-stage build with Eclipse Temurin 21 & Alpine Linux
# =========================================================================

# Stage 1: Build Application JAR
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Cache Maven dependencies layer
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source and package executable JAR
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# Stage 2: Secure, Minimal JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install font libraries required by OpenPDF / iText for PDF rendering
RUN apk add --no-cache fontconfig freetype ttf-dejavu

# Create persistent storage folder and non-root service user
RUN mkdir -p /app/storage \
    && addgroup -S swasthai \
    && adduser -S swasthai -G swasthai \
    && chown -R swasthai:swasthai /app

USER swasthai

COPY --from=builder --chown=swasthai:swasthai /app/target/*.jar app.jar

ENV PORT=8087
EXPOSE 8087

ENTRYPOINT ["java", "-jar", "app.jar"]
