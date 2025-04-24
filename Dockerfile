FROM gradle:8.13-jdk17 as builder

WORKDIR /build

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN gradle clean jlink --no-daemon

# jdk container
FROM openjdk:17-jdk-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    libopenjfx-java \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Copy executable and config files
COPY --from=builder /build/build/image /app/image
#COPY src/main/resources/config.properties /app/config.properties

CMD ["/app/image/bin/tiptracker-app"]