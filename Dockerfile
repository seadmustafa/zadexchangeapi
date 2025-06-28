# Use OpenJDK 21 instead of 17
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY target/exchangeapi-*.jar app.jar

# Expose application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
