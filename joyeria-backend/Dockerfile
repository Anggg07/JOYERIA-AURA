# Multi-stage build for Spring Boot backend
FROM maven:3.9.15-eclipse-temurin-25-alpine AS builder
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S joyeria && adduser -S joyeria -G joyeria \
  && mkdir -p /app/uploads \
  && chown -R joyeria:joyeria /app
COPY --from=builder /app/target/*.jar app.jar
RUN chown joyeria:joyeria app.jar
USER joyeria
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
