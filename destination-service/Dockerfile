FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY shared-lib/pom.xml ./shared-lib/pom.xml
COPY shared-lib/src ./shared-lib/src
RUN mvn install -f shared-lib/pom.xml -DskipTests

COPY contracts/pom.xml ./contracts/pom.xml
COPY contracts/src ./contracts/src
RUN mvn install -f contracts/pom.xml -DskipTests

COPY destination-service/pom.xml ./destination-service/pom.xml
COPY destination-service/src ./destination-service/src
RUN mvn clean package -f destination-service/pom.xml -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/destination-service/target/destination-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
