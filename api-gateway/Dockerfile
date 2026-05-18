FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY shared-lib/pom.xml ./shared-lib/pom.xml
COPY shared-lib/src ./shared-lib/src
RUN mvn install -f shared-lib/pom.xml -DskipTests

COPY contracts/pom.xml ./contracts/pom.xml
COPY contracts/src ./contracts/src
RUN mvn install -f contracts/pom.xml -DskipTests

COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY api-gateway/src ./api-gateway/src
RUN mvn clean package -f api-gateway/pom.xml -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
