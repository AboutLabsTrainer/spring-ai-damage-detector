# syntax=docker/dockerfile:1
# Build needs Docker only. The API key is never part of the image: it comes from the run-time environment.
FROM eclipse-temurin:21.0.13_9-jdk-noble@sha256:114cabb73033ca4ce7ceec3deb62396bc1b716282bae051ed3c1d82e38fcab2d AS build
WORKDIR /build
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src/main src/main
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -Dmaven.test.skip=true package \
 && cp target/damage-detector-*.jar /build/app.jar

FROM eclipse-temurin:21.0.13_9-jre-noble@sha256:0eda5830ed7b669abc246a8dc520ea8a23cab06f9a8d4b6964109e2f2394e46f
ARG GIT_SHA=unknown
LABEL org.opencontainers.image.revision=$GIT_SHA
COPY --from=build /build/app.jar /app/app.jar
USER 1000:1000
ENTRYPOINT ["java","-jar","/app/app.jar"]
