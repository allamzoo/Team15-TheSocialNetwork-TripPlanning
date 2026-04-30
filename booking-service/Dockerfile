FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY shared-lib/pom.xml ./shared-lib/pom.xml
COPY shared-lib/src ./shared-lib/src
RUN mvn install -f shared-lib/pom.xml -DskipTests

COPY booking-service/pom.xml ./booking-service/pom.xml
COPY booking-service/src ./booking-service/src
RUN mvn clean package -f booking-service/pom.xml -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/booking-service/target/booking-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
