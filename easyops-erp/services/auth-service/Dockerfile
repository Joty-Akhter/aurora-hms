# Production Dockerfile for Auth Service
# Multi-stage: build JAR, then run in minimal JRE image

# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY services/auth-service/pom.xml services/auth-service/

# Resolve dependencies (cached unless pom changes)
RUN mvn dependency:go-offline -pl services/auth-service -am -B -q || true

COPY . .
RUN mvn clean package -pl services/auth-service -am -DskipTests -B -q

# Runtime stage — slim JRE, no Maven
FROM eclipse-temurin:21-jre
WORKDIR /app

# Optional: run as non-root
RUN groupadd -r app && useradd -r -g app app
USER app

COPY --from=build /app/services/auth-service/target/auth-service-1.0.0.jar app.jar

EXPOSE 8083

# Production JVM: memory, GC, and safe defaults
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
