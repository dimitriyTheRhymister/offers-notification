# ============ Build stage ============
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# 1. Копируем только pom.xml для кэширования зависимостей
COPY pom.xml ./
COPY offers-service/pom.xml offers-service/
COPY notification-service/pom.xml notification-service/

# 2. Скачиваем зависимости (кэшируется между сборками)
RUN mvn -B -pl notification-service -am dependency:go-offline

# 3. Копируем исходники и собираем
COPY notification-service/src notification-service/src
RUN mvn -B -pl notification-service -am package -DskipTests

# ============ Runtime stage ============
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Непривилегированный пользователь
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /build/notification-service/target/notification-service-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]