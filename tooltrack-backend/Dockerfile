FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 tooltrack
WORKDIR /app
COPY --from=build /workspace/target/tooltrack-backend-*.jar app.jar
RUN mkdir /data && chown tooltrack:tooltrack /data
USER tooltrack
ENV SPRING_PROFILES_ACTIVE=prod UPLOAD_DIR=/data/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
