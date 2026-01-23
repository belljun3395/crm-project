# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copy gradle wrapper and build files first for caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Grant execution rights on the gradlew
RUN chmod +x ./gradlew

# Copy source code
COPY src src

# Build the application (skipping tests for speed, assuming CI handles it)
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a non-root user
RUN addgroup --system --gid 1001 spring && \
    adduser --system --uid 1001 --ingroup spring spring

# Create logs directory and set permissions
RUN mkdir /logs && chown spring:spring /logs

# Copy the built jar from the builder stage
COPY --from=builder --chown=spring:spring /app/build/libs/*.jar app.jar

# Set environment variables
ENV PROFILE=default \
    TZ=Asia/Seoul

# Switch to non-root user
USER spring

# Expose port
EXPOSE 8080

# Entrypoint
ENTRYPOINT ["java", \
            "-XX:MaxGCPauseMillis=100", \
            "-XX:InitialRAMPercentage=50.0", \
            "-XX:MaxRAMPercentage=80.0", \
            "-jar", "app.jar"]
