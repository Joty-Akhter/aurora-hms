# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent pom and settings
COPY pom.xml .
COPY maven-settings.xml /root/.m2/settings.xml

# Copy entire project for multi-module build
COPY . .

# Build the Manufacturing service module
RUN mvn clean package -pl services/manufacturing-service -am -DskipTests spring-boot:repackage

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar
COPY --from=build /app/services/manufacturing-service/target/manufacturing-service-1.0.0.jar app.jar

# Expose port
EXPOSE 8098

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

