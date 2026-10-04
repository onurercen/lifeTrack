# ---- Build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Resolve dependencies in their own layer so code changes don't re-download them.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Run ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 lifetrack
USER lifetrack

COPY --from=build /app/target/lifetrack-backend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
