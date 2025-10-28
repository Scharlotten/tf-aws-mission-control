# Multi-stage build: Build stage
FROM docker.io/library/maven:3.8.6-openjdk-11 AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application and create fat JAR
RUN mvn clean package -DskipTests

# Runtime stage
FROM docker.io/library/openjdk:11-jre-slim

WORKDIR /app

# Copy the fat JAR from build stage (updated artifact name)
COPY --from=build /app/target/produce-cassandra-1.0.0-fat.jar app.jar

# Create non-root user for security
RUN addgroup --system appgroup && adduser --system --group appuser
USER appuser

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=80"

# Health check to ensure the application is running
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD pgrep -f java || exit 1

# Use exec form for better signal handling
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]