# Multi-stage build: Build with Maven, then run with JRE
# Using Eclipse Temurin base image (replacement for deprecated OpenJDK)
FROM eclipse-temurin:8-jdk AS builder

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage - Use Eclipse Temurin (replacement for deprecated OpenJDK images)
FROM eclipse-temurin:8-jre

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/target/fulfillment-system-1.0.0.jar app.jar

# Set JVM options for better performance
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Create a wrapper script to properly handle arguments
RUN echo '#!/bin/sh\nset -e\nexec java $JAVA_OPTS -jar /app/app.jar "$@"' > /app/entrypoint.sh && \
    chmod +x /app/entrypoint.sh

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser && \
    chown -R appuser:appuser /app
USER appuser

# Default command
ENTRYPOINT ["/app/entrypoint.sh"]
