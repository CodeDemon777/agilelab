# Multi-stage Dockerfile for AgileLab Spring Boot Backend on Render
# Stage 1: Build JAR using Maven
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Production JRE Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy built artifact from builder stage
COPY --from=builder /app/target/chatapp-1.0.0.jar app.jar

# Render sets $PORT dynamically (defaults to 8088 if not set)
ENV PORT=8088
EXPOSE ${PORT}

# Run Spring Boot application
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]

