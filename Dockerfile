# -------- BUILD STAGE ---------
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
COPY src ./src

RUN ./gradlew build -x test

# -------- RUNTIME STAGE ---------
FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar /app/ping-watch.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "ping-watch.jar"]