# -------- Build stage --------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper + pom first for better layer caching
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Download dependencies (cached if pom.xml unchanged)
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

# Copy source and build jar
COPY src src
RUN ./mvnw -q -DskipTests clean package

# -------- Runtime stage --------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Optional JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

# Copy built Spring Boot jar
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]