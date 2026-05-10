# syntax=docker/dockerfile:1.7
# Build from repository root: docker compose uses context: . and dockerfile: backend/Dockerfile
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY pom.xml .
COPY foot-traffic-simulation-core/pom.xml foot-traffic-simulation-core/
COPY foot-traffic-simulation-contract/pom.xml foot-traffic-simulation-contract/
COPY soap-foot-traffic-server/pom.xml soap-foot-traffic-server/
COPY rmi-scoring-api/pom.xml rmi-scoring-api/
COPY rmi-scoring-server/pom.xml rmi-scoring-server/
COPY backend/pom.xml backend/
COPY ai-orchestrator/pom.xml ai-orchestrator/
COPY selenium-e2e/pom.xml selenium-e2e/

COPY foot-traffic-simulation-core/src foot-traffic-simulation-core/src
COPY foot-traffic-simulation-contract/src foot-traffic-simulation-contract/src
COPY rmi-scoring-api/src rmi-scoring-api/src
COPY rmi-scoring-server/src rmi-scoring-server/src
COPY backend/src backend/src
COPY ai-orchestrator/src ai-orchestrator/src
COPY selenium-e2e/src selenium-e2e/src

# Same as rmi-scoring-server/Dockerfile (bridge DNS to Maven Central)
#
# Notes:
# - This step often "looks stuck" because of `-q` (quiet) + long dependency downloads / tests.
# - We mount the Maven local repo as a BuildKit cache to avoid re-downloading dependencies.
# - `package` already runs tests; no need for `test package`.
ARG RUN_TESTS=true
RUN --mount=type=cache,target=/root/.m2 \
  for i in 1 2 3 4 5; do \
    if [ "$RUN_TESTS" = "true" ]; then \
      mvn -B -V -pl backend -am package && break; \
    else \
      mvn -B -V -pl backend -am -DskipTests package && break; \
    fi; \
    if [ "$i" -eq 5 ]; then echo "mvn failed after 5 attempts"; exit 1; fi; \
    echo "mvn failed (attempt $i/5), retrying in 8s..."; \
    sleep 8; \
  done

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

COPY --from=build /build/backend/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
