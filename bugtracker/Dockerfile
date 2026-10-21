# ============================================================
# Stage 1: Build
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Fix permission denied error on Linux
# Windows does not preserve execute permissions
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build JAR
RUN ./mvnw clean package -DskipTests -B

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create upload directory
RUN mkdir -p /tmp/bugtracker-uploads

# Copy JAR from builder stage
COPY --from=builder /app/target/bugtracker-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
            "-Xms256m", \
            "-Xmx512m", \
            "-XX:+UseContainerSupport", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]