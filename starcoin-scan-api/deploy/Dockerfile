FROM openjdk:8-jdk-alpine as build
WORKDIR /workspace/app

COPY ../../mvnw .
COPY ../../.mvn .mvn
COPY ../../pom.xml .
COPY ../../starcoin-commons starcoin-commons
COPY ../../starcoin-indexer starcoin-indexer
COPY ../../starcoin-scan-api starcoin-scan-api
COPY settings.xml .

RUN --mount=type=cache,target=/root/.m2 ./mvnw --settings settings.xml install -DskipTests --quiet

FROM openjdk:8-jdk-alpine
ARG STARCOIN_ES_URL
ARG STARCOIN_ES_PROTOCOL
ARG STARCOIN_ES_PORT
ARG STARCOIN_ES_USER
ARG STARCOIN_ES_PWD
ARG STARCOIN_ES_INDEX_VERSION

ENV STARCOIN_ES_URL=$STARCOIN_ES_URL
ENV STARCOIN_ES_PROTOCOL=$STARCOIN_ES_PROTOCOL
ENV STARCOIN_ES_PORT=$STARCOIN_ES_PORT
ENV STARCOIN_ES_USER=$STARCOIN_ES_USER
ENV STARCOIN_ES_PWD=$STARCOIN_ES_PWD
ENV STARCOIN_ES_INDEX_VERSION=$STARCOIN_ES_INDEX_VERSION
RUN addgroup -S starcoin && adduser -S starcoin -G starcoin
VOLUME /tmp
RUN mkdir logs && chown -R starcoin:starcoin logs
USER starcoin
ARG DEPENDENCY=/workspace/app/starcoin-scan-api/target
COPY --from=build ${DEPENDENCY}/starcoin-scan-api-1.1.jar /app/lib/scan-api-1.1.jar
ENTRYPOINT ["java","-noverify","-XX:TieredStopAtLevel=1","-jar","app/lib/scan-api-1.1.jar","-Dspring.main.lazy-initialization=true","STARCOIN_ES_URL=$STARCOIN_ES_URL","STARCOIN_ES_PROTOCOL=$STARCOIN_ES_PROTOCOL","STARCOIN_ES_PORT=$STARCOIN_ES_PORT","STARCOIN_ES_USER=$STARCOIN_ES_USER","STARCOIN_ES_PWD=$STARCOIN_ES_PWD","STARCOIN_ES_INDEX_VERSION$STARCOIN_ES_INDEX_VERSION"]