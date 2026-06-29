FROM eclipse-temurin:26-jdk AS build
LABEL authors="yamadaminorutou"
WORKDIR /app
COPY . .
RUN ./gradlew bootJar

FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]


