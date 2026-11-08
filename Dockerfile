# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
# Tests are skipped here because they need a running PostgreSQL; run them with ./mvnw test before building.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package && cp target/*.jar app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /workspace/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
