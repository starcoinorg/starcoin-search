FROM openjdk:11-jdk-alpine as build
WORKDIR /workspace/app

COPY ../../mvnw .
COPY ../../.mvn .mvn
COPY ../../pom.xml .
COPY ../../starcoin-commons starcoin-commons
COPY ../../starcoin-indexer starcoin-indexer
COPY ../../starcoin-scan-api starcoin-scan-api
COPY settings.xml .

RUN --mount=type=cache,target=/root/.m2 ./mvnw --settings settings.xml install -DskipTests --quiet

FROM openjdk:11-jdk-alpine
ARG HOSTS
ARG NETWORK
ARG BG_TASK_JOBS
ARG BULK_SIZE
ARG STARCOIN_ES_PWD
ARG STARCOIN_ES_URL
ARG STARCOIN_ES_PROTOCOL
ARG STARCOIN_ES_PORT
ARG STARCOIN_ES_USER
ARG SWAP_API_URL
ARG SWAP_CONTRACT_ADDR
ARG DS_URL
ARG DB_USER_NAME
ARG DB_PWD
ARG DB_SCHEMA

ARG PROGRAM_ARGS

ENV HOSTS=$HOSTS
ENV NETWORK=$NETWORK
ENV BG_TASK_JOBS=$BG_TASK_JOBS
ENV BULK_SIZE=$BULK_SIZE
ENV STARCOIN_ES_PWD=$STARCOIN_ES_PWD
ENV STARCOIN_ES_URL=$STARCOIN_ES_URL
ENV STARCOIN_ES_PROTOCOL=$STARCOIN_ES_PROTOCOL
ENV STARCOIN_ES_PORT=$STARCOIN_ES_PORT
ENV STARCOIN_ES_USER=$STARCOIN_ES_USER
ENV SWAP_API_URL=$SWAP_API_URL
ENV SWAP_CONTRACT_ADDR=$SWAP_CONTRACT_ADDR
ENV DS_URL=$DS_URL
ENV DB_USER_NAME=$DB_USER_NAME
ENV DB_PWD=$DB_PWD
ENV DB_SCHEMA=$DB_SCHEMA

ENV PROGRAM_ARGS=$PROGRAM_ARGS


RUN addgroup -S starcoin && adduser -S starcoin -G starcoin
VOLUME /tmp
RUN mkdir logs && chown -R starcoin:starcoin logs
USER starcoin
ARG DEPENDENCY=/workspace/app/starcoin-indexer/target/
COPY --from=build ${DEPENDENCY}/starcoin-indexer-1.1.jar /app/lib/indexer.jar
ENTRYPOINT ["java","-noverify","-jar","app/lib/indexer.jar", \
            "PROGRAM_ARGS=$PROGRAM_ARGS", \
            "HOSTS=$HOSTS", \
            "NETWORK=$NETWORK", \
            "BG_TASK_JOBS=$BG_TASK_JOBS", \
            "BULK_SIZE=$BULK_SIZE", \
            "STARCOIN_ES_PWD=$STARCOIN_ES_PWD", \
            "STARCOIN_ES_URL=$STARCOIN_ES_URL", \
            "STARCOIN_ES_PROTOCOL=$STARCOIN_ES_PROTOCOL", \
            "STARCOIN_ES_PORT=$STARCOIN_ES_PORT", \
            "STARCOIN_ES_USER=$STARCOIN_ES_USER", \
            "SWAP_API_URL=$SWAP_API_URL", \
            "SWAP_CONTRACT_ADDR=$SWAP_CONTRACT_ADDR", \
            "DS_URL=$DS_URL", \
            "DB_USER_NAME=$DB_USER_NAME", \
            "DB_PWD=$DB_PWD", \
            "DB_SCHEMA=$DB_SCHEMA" \
            ]