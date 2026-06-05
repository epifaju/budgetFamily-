# Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S invoiceai && adduser -S invoiceai -G invoiceai
USER invoiceai
COPY --from=build /app/target/invoice-ai-backend-*.jar app.jar
EXPOSE 8085
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
