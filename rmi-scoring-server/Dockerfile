# Build from repository root: context ., dockerfile rmi-scoring-server/Dockerfile
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY pom.xml .
COPY rmi-scoring-api/pom.xml rmi-scoring-api/
COPY rmi-scoring-server/pom.xml rmi-scoring-server/
# Parent pom lists backend as a module; Maven requires the directory to exist even when building only RMI.
COPY backend/pom.xml backend/

COPY rmi-scoring-api/src rmi-scoring-api/src
COPY rmi-scoring-server/src rmi-scoring-server/src

RUN mvn -q -pl rmi-scoring-server -am -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache netcat-openbsd

ENV RMI_REGISTRY_PORT=1099
ENV RMI_EXPORT_PORT=45000
ENV RMI_SERVER_HOSTNAME=rmi-scoring

COPY --from=build /build/rmi-scoring-server/target/rmi-scoring-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 1099 45000

ENTRYPOINT ["sh", "-c", "exec java -Djava.rmi.server.hostname=${RMI_SERVER_HOSTNAME} -jar /app/app.jar"]
