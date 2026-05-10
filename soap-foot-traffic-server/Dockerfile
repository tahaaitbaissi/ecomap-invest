# syntax=docker/dockerfile:1.7
# Build from repository root: docker compose uses context: . and dockerfile: soap-foot-traffic-server/Dockerfile
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY pom.xml .
COPY foot-traffic-simulation-core/pom.xml foot-traffic-simulation-core/
COPY foot-traffic-simulation-contract/pom.xml foot-traffic-simulation-contract/
COPY soap-foot-traffic-server/pom.xml soap-foot-traffic-server/

COPY foot-traffic-simulation-core/src foot-traffic-simulation-core/src
COPY foot-traffic-simulation-contract/src foot-traffic-simulation-contract/src
COPY soap-foot-traffic-server/src soap-foot-traffic-server/src

# Root pom.xml lists every module; only the foot-traffic chain is needed — copy minimal stub POMs
# for sibling modules so Maven can parse the aggregator (see build-support/docker-stub-poms/).
COPY build-support/docker-stub-poms/rmi-scoring-api/pom.xml rmi-scoring-api/pom.xml
COPY build-support/docker-stub-poms/rmi-scoring-server/pom.xml rmi-scoring-server/pom.xml
COPY build-support/docker-stub-poms/backend/pom.xml backend/pom.xml
COPY build-support/docker-stub-poms/ai-orchestrator/pom.xml ai-orchestrator/pom.xml
COPY build-support/docker-stub-poms/selenium-e2e/pom.xml selenium-e2e/pom.xml

ARG RUN_TESTS=false
RUN --mount=type=cache,target=/root/.m2 \
  if [ "$RUN_TESTS" = "true" ]; then \
    mvn -B -V -pl soap-foot-traffic-server -am package; \
  else \
    mvn -B -V -pl soap-foot-traffic-server -am -DskipTests package; \
  fi

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
COPY --from=build /build/soap-foot-traffic-server/target/soap-foot-traffic-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8092
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
