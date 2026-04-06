FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/q-scout-for-spring-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dserver.address=0.0.0.0 -jar /app/app.jar --server.port=${PORT:-8080}"]
