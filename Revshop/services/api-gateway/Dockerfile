FROM eclipse-temurin:26-jdk-alpine
LABEL authors="Hritika"

WORKDIR /app

COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "app.jar"]