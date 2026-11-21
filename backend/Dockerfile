FROM eclipse-temurin:21-jdk-alpine AS build
ENV TZ=America/Sao_Paulo
WORKDIR /app
COPY mvnw .
RUN chmod +x mvnw
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
