FROM node:22-bookworm-slim AS frontend
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
# Sólo configuración pública: nunca pasar secretos a Vite.
ARG VITE_MAPTILER_KEY
ARG VITE_MAP_TILE_URL
ARG VITE_MAP_ATTRIBUTION
RUN npm run build

FROM eclipse-temurin:21-jdk-jammy AS backend
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends nginx gettext-base \
    && rm -rf /var/lib/apt/lists/* /etc/nginx/sites-enabled/default
WORKDIR /app
COPY --from=backend /build/target/rodatours-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --from=frontend /build/frontend/dist/ /usr/share/nginx/html/
COPY ops/render/nginx.conf.template /app/nginx.conf.template
COPY ops/render/start.sh /app/start.sh
RUN chmod +x /app/start.sh
ENV PORT=10000
EXPOSE 10000
ENTRYPOINT ["/bin/bash", "/app/start.sh"]
