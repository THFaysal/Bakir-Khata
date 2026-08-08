# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-26-noble AS build
WORKDIR /app

# Cache dependencies first
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Run stage ----------
FROM eclipse-temurin:26-jre-noble AS run
WORKDIR /app

# Run as non-root user
RUN useradd -ms /bin/bash spring
USER spring

# Copy the built jar from the build stage
COPY --from=build /app/target/Bakir_Khata-*.jar app.jar

# Railway injects PORT at runtime; default to 8080 for local docker run
ENV PORT=8080
EXPOSE 8080

# Use shell form so $PORT is expanded at container start,
# and bind Spring Boot to it via server.port
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]