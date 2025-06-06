# Use a slim Java 21 runtime image
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the JAR file into the image
COPY target/*.jar app.jar

# Expose the port your Spring Boot app uses
EXPOSE 8080

# Command to run your app
ENTRYPOINT ["java", "-jar", "app.jar"]