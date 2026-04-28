FROM gradle:9.4.1-jdk21 AS builder

WORKDIR /workspace

COPY settings.gradle.kts build.gradle.kts ./
COPY src/ src/

RUN gradle --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/* \
	&& useradd --create-home --shell /bin/bash appuser

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
