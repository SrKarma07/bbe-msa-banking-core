
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts build.gradle.kts
COPY settings.gradle settings.gradle

RUN ./gradlew --no-daemon dependencies || true

COPY src src

RUN ./gradlew --no-daemon clean openApiGenerate bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8091
ENTRYPOINT ["java","-Dreactor.netty.http.server.accessLogEnabled=true","-jar","/app/app.jar"]
