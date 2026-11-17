#!/usr/bin/env bash
# Levanta los seis microservicios Flask y el monolito en segundo plano en la VM.
# Requisitos: PostgreSQL y Redis activos, .env configurados, .venv con dependencias y npm install hecho.
# Logs en logs/<servicio>.log. Para detener: ./levanta_todo.sh stop
set -u

# Funciona tanto en la raiz del repo como dentro de scripts_vm/.
REPO="$(cd "$(dirname "$0")" && pwd)"
[ -d "$REPO/apps/services" ] || REPO="$(dirname "$REPO")"
[ -d "$REPO/apps/services" ] || { echo "No encuentro apps/services; coloca el script en la raiz del repo (library/)."; exit 1; }
cd "$REPO"
mkdir -p logs

if [ "${1:-}" = "stop" ]; then
  pkill -f "apps/services/.*/app.py"
  pkill -f "apps/web-monolito/src/server.js"
  echo "Servicios detenidos."
  exit 0
fi

PY="${PYTHON:-.venv/bin/python}"
[ -x "$PY" ] || PY=python3

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
echo "200 = ok, 503 = falta PostgreSQL o Redis, sin respuesta = revisa logs/<servicio>.log"
