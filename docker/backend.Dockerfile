# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /source
ENV MAVEN_OPTS="-Xmx1024m"
COPY zaomeng-backend/server/ ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -pl yudao-server -am package \
    -Dtest=FlatFileStorageTest,R2FileClientTest,ObjectStorageBootstrapTest,GenerationReferenceStorageTest,GenerationSubmissionTest,RedisConnectionConfigurationTest,EmailAuthServiceTest -Dsurefire.failIfNoSpecifiedTests=false

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd -g 10001 safillu && useradd -u 10001 -g safillu -M safillu \
    && mkdir -p /app/uploads /app/logs && chown -R safillu:safillu /app
WORKDIR /app
COPY --from=build /source/yudao-server/target/yudao-server.jar /app/server.jar
COPY docker/backend-entrypoint.sh /app/entrypoint.sh
RUN sed -i 's/\r$//' /app/entrypoint.sh && chmod 755 /app/entrypoint.sh
# Secret bind mounts must remain readable to uid 10001; init sets their group.
USER 10001:10001
EXPOSE 48080
ENTRYPOINT ["/app/entrypoint.sh"]
