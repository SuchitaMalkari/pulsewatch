# One container serves both the React dashboard and the Spring Boot API.

# 1. Build the dashboard
FROM node:22-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# 2. Build the API, with the dashboard bundled in as static files
FROM maven:3.9-eclipse-temurin-17 AS api
WORKDIR /app
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=web /web/dist/ ./src/main/resources/static/
# Tests already run in GitHub Actions on every push.
RUN mvn -B -q package -DskipTests

# 3. Small runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=api /app/target/pulsewatch-backend-*.jar app.jar
# Demo deployment: in-memory database with the built-in demo services.
ENV SPRING_PROFILES_ACTIVE=h2
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
