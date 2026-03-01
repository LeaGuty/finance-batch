FROM eclipse-temurin:21-jdk-alpine
LABEL maintainer="Duoc UC - Backend 3"
WORKDIR /app
# ¡Solo cambia esta línea en cada proyecto!
COPY target/finance-batch-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]