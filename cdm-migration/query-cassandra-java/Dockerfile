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

# Copy the fat JAR from build stage
COPY --from=build /app/target/query-cassandra-1.0.0-fat.jar app.jar

# Create non-root user for security
RUN addgroup --system appgroup && adduser --system --group appuser
USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]