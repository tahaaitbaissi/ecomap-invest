# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY pom.xml .
# Parent aggregator lists foot-traffic modules; copy minimal POMs so Maven can parse the reactor.
COPY build-support/docker-stub-poms/foot-traffic-simulation-core/pom.xml foot-traffic-simulation-core/pom.xml
COPY build-support/docker-stub-poms/foot-traffic-simulation-contract/pom.xml foot-traffic-simulation-contract/pom.xml
COPY build-support/docker-stub-poms/soap-foot-traffic-server/pom.xml soap-foot-traffic-server/pom.xml
COPY rmi-scoring-api/pom.xml rmi-scoring-api/
COPY rmi-scoring-server/pom.xml rmi-scoring-server/
COPY backend/pom.xml backend/
COPY ai-orchestrator/pom.xml ai-orchestrator/
COPY selenium-e2e/pom.xml selenium-e2e/

COPY rmi-scoring-api/src rmi-scoring-api/src
COPY rmi-scoring-server/src rmi-scoring-server/src
COPY backend/src backend/src
COPY ai-orchestrator/src ai-orchestrator/src
COPY selenium-e2e/src selenium-e2e/src

ARG RUN_TESTS=true
RUN --mount=type=cache,target=/root/.m2 \
  for i in 1 2 3 4 5; do \
    if [ "$RUN_TESTS" = "true" ]; then \
      mvn -B -V -pl ai-orchestrator -am package && break; \
    else \
      mvn -B -V -pl ai-orchestrator -am -DskipTests package && break; \
    fi; \
    if [ "$i" -eq 5 ]; then echo "mvn failed after 5 attempts"; exit 1; fi; \
    echo "mvn failed (attempt $i/5), retrying in 8s..."; \
    sleep 8; \
  done

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

COPY --from=build /build/ai-orchestrator/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

