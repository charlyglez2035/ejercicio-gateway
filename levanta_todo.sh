#!/usr/bin/env bash
# Levanta los seis microservicios Flask y el monolito en segundo plano (para la VM GCP o WSL).
# Requisitos: PostgreSQL y Redis activos, .env configurado, .venv con dependencias y npm install hecho.
# Logs en logs/<servicio>.log. Para detener: ./levanta_todo.sh stop
set -u
cd "$(dirname "$0")"
mkdir -p logs

if [ "${1:-}" = "stop" ]; then
  pkill -f "apps/services/.*/app.py"
  pkill -f "apps/web-monolito/src/server.js"
  echo "Servicios detenidos."
  exit 0
fi

PY="${PYTHON:-.venv/bin/python}"
[ -x "$PY" ] || PY=python3

# Detras del API Gateway nginx los servicios solo escuchan en localhost.
# Las variables exportadas ganan sobre los .env (load_dotenv/dotenv no sobrescriben).
# Para exponerlos directo como antes: BIND_HOST=0.0.0.0 ./levanta_todo.sh
BIND_HOST="${BIND_HOST:-127.0.0.1}"
export SERVICE_HOST="$BIND_HOST" LOGIN_HOST="$BIND_HOST" HOST="$BIND_HOST"

for s in login soap users authors pedidos pagos; do
  nohup "$PY" "apps/services/$s/app.py" > "logs/$s.log" 2>&1 &
  echo "$s -> PID $!"
done
nohup npm start > logs/monolito.log 2>&1 &
echo "monolito -> PID $!"

sleep 5
for p in 5000 5001 5002 5003 5004 5005; do
  printf "%s: " "$p"
  curl -s -o /dev/null -w "%{http_code}\n" "http://127.0.0.1:$p/health?format=json" || echo "sin respuesta"
done
printf "3000: "
curl -s -o /dev/null -w "%{http_code}\n" "http://127.0.0.1:3000/" || echo "sin respuesta"

if command -v nginx >/dev/null 2>&1; then
  echo "--- API Gateway (nginx :80) ---"
  for path in /gateway/health /api/books/health /api/login/health /api/users/health \
              /api/authors/health /api/pedidos/health /api/pagos/health /api/users/users; do
    printf "%s: " "$path"
    curl -s -o /dev/null -w "%{http_code}\n" "http://127.0.0.1$path?format=json" || echo "sin respuesta"
  done
  echo "(/api/users/users sin token debe dar 401)"
fi
