FROM eclipse-temurin:26-jdk-alpine
LABEL authors="Hritika"

WORKDIR /app

COPY target/order-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]