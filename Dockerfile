# ====================================================================
# Multi-Stage Dockerfile for Render Web Service Deployment
# Java 17 Spring Boot + Render PostgreSQL / MySQL
# ====================================================================

# Stage 1: Build the Application with Maven & JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy Maven POM and dependency descriptors
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy backend source code (including static frontend resources)
COPY backend/src ./src

# Package production executable JAR without running tests during image build
RUN mvn clean package -DskipTests

# Stage 2: Minimal Production JRE Image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create unprivileged application user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy built JAR from builder stage
COPY --from=builder /app/target/doctor-scheduling-system-1.0.0.jar app.jar

# Expose Render default port (8080 or PORT env variable)
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot application
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
