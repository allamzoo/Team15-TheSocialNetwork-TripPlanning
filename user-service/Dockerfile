FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY shared-lib/pom.xml ./shared-lib/pom.xml
COPY shared-lib/src ./shared-lib/src
RUN mvn install -f shared-lib/pom.xml -DskipTests

COPY user-service/pom.xml ./user-service/pom.xml
COPY user-service/src ./user-service/src
RUN mvn clean package -f user-service/pom.xml -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/user-service/target/user-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
