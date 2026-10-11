# syntax=docker/dockerfile:1
FROM node:24-bookworm-slim AS node

FROM eclipse-temurin:25-jdk-noble AS build
COPY --from=node /usr/local/ /usr/local/
WORKDIR /build
COPY . .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.npm \
    ./mvnw -B -f shared/pom.xml clean install && \
    ./mvnw -B -Pproduction -Dvaadin.productionMode=true -Dvaadin.forceProductionBuild=true clean verify

FROM eclipse-temurin:25-jre-noble AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app \
    && mkdir -p /app/data /app/logs && chown -R app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/app-1.0-SNAPSHOT.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
