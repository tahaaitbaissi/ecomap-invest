FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY pom.xml .
COPY rmi-scoring-api/pom.xml rmi-scoring-api/
COPY rmi-scoring-server/pom.xml rmi-scoring-server/
COPY backend/pom.xml backend/
COPY ai-orchestrator/pom.xml ai-orchestrator/

COPY rmi-scoring-api/src rmi-scoring-api/src
COPY rmi-scoring-server/src rmi-scoring-server/src
COPY backend/src backend/src
COPY ai-orchestrator/src ai-orchestrator/src

RUN mvn -B -V -q -pl ai-orchestrator -am clean test package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

COPY --from=build /build/ai-orchestrator/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

