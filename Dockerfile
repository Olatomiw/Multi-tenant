FROM maven:3.9-eclipse-temurin-17 AS builder
LABEL authors="RLS"

WORKDIR /app

# Copy pom first and resolve deps — this layer gets cached
# as long as pom.xml doesn't change
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q



FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

#Copy the built jar
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]