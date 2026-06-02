FROM maven:3-eclipse-temurin-26 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S futbol && adduser -S futbol -G futbol
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
USER futbol
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider "http://localhost:8080/futbix/v1/teams?page=0&size=1" || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
