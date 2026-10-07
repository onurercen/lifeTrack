# Builds the Flutter web app and serves it with Caddy (see deploy/Caddyfile).
# Flutter is installed from its repository so the version matches CI exactly.
ARG FLUTTER_VERSION=3.47.4

# ---- Build ----
FROM debian:bookworm-slim AS build
ARG FLUTTER_VERSION
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl git unzip xz-utils \
    && rm -rf /var/lib/apt/lists/*
RUN git clone --depth 1 --branch "${FLUTTER_VERSION}" https://github.com/flutter/flutter.git /opt/flutter
ENV PATH="/opt/flutter/bin:${PATH}"
RUN flutter config --no-analytics --enable-web && flutter precache --web

WORKDIR /app
COPY pubspec.yaml pubspec.lock ./
RUN flutter pub get

COPY . .
# Same origin as the API: the browser resolves /api against the site's address.
RUN flutter build web --release --dart-define=API_BASE_URL=/api

# ---- Serve ----
FROM caddy:2
COPY --from=build /app/build/web /srv
