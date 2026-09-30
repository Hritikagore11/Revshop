FROM eclipse-temurin:26-jdk-alpine
LABEL authors="Hritika"

WORKDIR /app

COPY target/payment-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]