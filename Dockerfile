# =========================
# BUILD STAGE
# =========================
FROM maven:3.9.16-eclipse-temurin-26 AS build

WORKDIR /app

# Copy pom first for dependency caching
COPY pom.xml .

RUN mvn -B -DskipTests dependency:go-offline

# Copy application source
COPY src ./src

# Build Spring Boot jar
RUN mvn -B clean package -DskipTests


# =========================
# RUNTIME STAGE
# =========================
FROM eclipse-temurin:26-jre

WORKDIR /app

# Create non-root user
RUN useradd --create-home --shell /bin/bash spring

# Copy built jar
COPY --from=build /app/target/*.jar app.jar

RUN chown spring:spring /app/app.jar

USER spring

# Render provides PORT automatically
ENV PORT=10000

# Useful for Render free-memory limits
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

EXPOSE 10000

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]