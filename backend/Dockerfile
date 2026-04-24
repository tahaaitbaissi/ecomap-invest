# Build from repository root: docker compose uses context: . and dockerfile: backend/Dockerfile
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY pom.xml .
COPY rmi-scoring-api/pom.xml rmi-scoring-api/
COPY rmi-scoring-server/pom.xml rmi-scoring-server/
COPY backend/pom.xml backend/

COPY rmi-scoring-api/src rmi-scoring-api/src
COPY rmi-scoring-server/src rmi-scoring-server/src
COPY backend/src backend/src

# Same as rmi-scoring-server/Dockerfile (bridge DNS to Maven Central)
RUN for i in 1 2 3 4 5; do \
  if mvn -B -V -q -pl backend -am clean test package; then \
    break; \
  fi; \
  if [ "$i" -eq 5 ]; then \
    echo "mvn failed after 5 attempts"; \
    exit 1; \
  fi; \
  echo "mvn failed (attempt $i/5), retrying in 8s..."; \
  sleep 8; \
done

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

COPY --from=build /build/backend/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
