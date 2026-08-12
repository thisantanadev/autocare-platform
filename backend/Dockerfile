# ---------- build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Dependencies are resolved before the sources are copied so that a code-only
# change reuses the cached Maven layer instead of downloading the world again.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package && \
    mv target/backend-*.jar target/app.jar

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl is only needed for the container health check below.
RUN apt-get update && \
    apt-get install --no-install-recommends -y curl && \
    rm -rf /var/lib/apt/lists/* && \
    useradd --system --create-home --shell /usr/sbin/nologin autocare

COPY --from=build /build/target/app.jar app.jar
RUN chown -R autocare:autocare /app
USER autocare

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
