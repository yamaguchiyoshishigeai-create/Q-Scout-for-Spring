FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system qscout \
    && useradd --system --gid qscout --create-home --home-dir /home/qscout qscout \
    && mkdir -p /tmp/qscout \
    && chown -R qscout:qscout /app /tmp/qscout

COPY --from=build /workspace/target/q-scout-for-spring-0.1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

USER qscout

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
