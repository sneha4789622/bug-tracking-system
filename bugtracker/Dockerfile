# ============================================================
# Dockerfile for Bug Tracking System
# Multi-stage build:
#   Stage 1 (builder): Compiles the Java code into a JAR
#   Stage 2 (runtime): Runs the JAR in a minimal JRE image
#
# Multi-stage builds keep the final image small — the build
# tools (Maven, JDK) are not included in the deployed image.
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory inside the container
WORKDIR /app

# Copy Maven wrapper and pom.xml first.
# Docker caches layers — if pom.xml has not changed,
# the dependency download layer is reused on next build.
# This makes rebuilds much faster.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download all dependencies (cached layer)
# -B = batch mode (no interactive prompts)
# dependency:go-offline = download everything needed
RUN ./mvnw dependency:go-offline -B

# Copy all source code
COPY src ./src

# Build the JAR, skipping tests
# (Tests were already run in the CI pipeline)
RUN ./mvnw clean package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────
# Use JRE (not JDK) — smaller image, no compiler needed at runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
# Running as root in a container is a security risk
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create upload directory and set permissions
RUN mkdir -p /tmp/bugtracker-uploads \
    && chown appuser:appgroup /tmp/bugtracker-uploads

# Copy only the built JAR from the builder stage
# This keeps the final image clean (no source code, no Maven)
COPY --from=builder /app/target/bugtracker-0.0.1-SNAPSHOT.jar app.jar

# Switch to non-root user
USER appuser

# Expose the port the app runs on
EXPOSE 8080

# Health check — Docker uses this to know if the container is healthy
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Start the application
# SPRING_PROFILES_ACTIVE is set as environment variable on Render
ENTRYPOINT ["java", \
            "-Xms256m", \
            "-Xmx512m", \
            "-XX:+UseContainerSupport", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]