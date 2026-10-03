# Imagen común para los microservicios. Uso: docker build --build-arg SERVICE=order-service .
FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /app
COPY pom.xml .
COPY events events
COPY order-service order-service
COPY inventory-service inventory-service
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl ${SERVICE} -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
ARG SERVICE
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /app/${SERVICE}/target/${SERVICE}-*.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
