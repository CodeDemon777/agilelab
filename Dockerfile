# Multi-stage Dockerfile for AgileLab Spring Boot Backend on Render
# Stage 1: Build JAR using Maven
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy pom.xml and source code directly
COPY pom.xml .
COPY src ./src

# Package production fat JAR without running tests
RUN mvn clean package -DskipTests --no-transfer-progress

# Stage 2: Minimal Production JRE Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user and pre-create data directory with full permissions
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    mkdir -p /app/data && \
    chown -R appuser:appgroup /app

# Copy built artifact from builder stage with correct ownership
COPY --from=builder --chown=appuser:appgroup /app/target/chatapp-1.0.0.jar /app/app.jar

# Switch to non-root user
USER appuser

# Render sets $PORT dynamically (defaults to 10000 on Render)
ENV PORT=10000
EXPOSE 10000

# Run Spring Boot application
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-10000} -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
