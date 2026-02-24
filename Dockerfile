# Fetching base image with java version 17
FROM bellsoft/liberica-openjdk-alpine:17

# Setting up work directory
WORKDIR /hcc-service

# Copy the jar file into work directory
COPY target/hcc-1.0-SNAPSHOT.jar /hcc-service

# Expose the port 9001
EXPOSE 8080

# Starting the application
CMD ["java", "-jar", "hcc-1.0-SNAPSHOT.jar"]