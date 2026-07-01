FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY modules ./modules
COPY apps ./apps
ARG MODULE
RUN mvn --batch-mode --no-transfer-progress -pl ${MODULE} -am package -DskipTests \
    && cp ${MODULE}/target/*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 copilot
WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar
USER copilot
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
