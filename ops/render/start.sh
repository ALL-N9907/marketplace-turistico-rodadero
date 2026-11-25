#!/usr/bin/env bash
set -Eeuo pipefail

if [[ ! "${PORT:-}" =~ ^[0-9]+$ ]] || (( PORT < 1 || PORT > 65535 || PORT == 8080 )); then
    echo 'PORT debe ser un puerto válido distinto del puerto interno 8080.' >&2
    exit 1
fi

# Sustituir sólo PORT; conservar las variables propias de nginx ($host, etc.).
envsubst '${PORT}' < /app/nginx.conf.template > /etc/nginx/conf.d/rodatours.conf
nginx -t

# Java queda accesible únicamente desde nginx dentro del contenedor.
java -jar /app/app.jar --server.address=127.0.0.1 --server.port=8080 &
java_pid=$!
nginx -g 'daemon off;' &
nginx_pid=$!

cleanup() {
    kill -TERM "$java_pid" "$nginx_pid" 2>/dev/null || true
    wait "$java_pid" "$nginx_pid" 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 143' TERM
trap 'exit 130' INT

# Si cualquiera falla, detener el contenedor completo para que Render lo reinicie.
set +e
wait -n "$java_pid" "$nginx_pid"
status=$?
set -e
if (( status == 0 )); then status=1; fi
exit "$status"
