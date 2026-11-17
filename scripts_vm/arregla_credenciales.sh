#!/usr/bin/env bash
# Repara DB_PASSWORD y REDIS_URL de ejercicio-gateway en la VM y reinicia los servicios.
#   bash scripts_vm/arregla_credenciales.sh
# - Busca credenciales que funcionen: config de Redis, .env de los demas repos en ~ y,
#   si ninguna sirve, las pide por teclado (sin eco).
# - Prueba cada candidato con el Python del .venv (psycopg / redis) antes de usarlo.
# - Respalda cada .env como .env.bak-<fecha> antes de modificarlo.
# - Nunca imprime contrasenas.
set -uo pipefail
cd "$(dirname "$0")/.."
ROOT=$(pwd)
PY="$ROOT/.venv/bin/python"
[ -x "$PY" ] || { echo "No existe $PY (crea el venv o enlaza ~/ejercicio-redis/.venv)."; exit 1; }
STAMP=$(date +%Y%m%d-%H%M%S)

get_var() { grep -m1 "^$2=" "$1" 2>/dev/null | cut -d= -f2-; }

db_ok() {  # $1=user $2=password
  DBU="$1" DBP="$2" DBH="$(get_var "$ROOT/.env" DB_HOST)" DBN="$(get_var "$ROOT/.env" DB_NAME)" \
  "$PY" - <<'EOF' >/dev/null 2>&1
import os, psycopg
psycopg.connect(host=os.environ["DBH"] or "localhost", dbname=os.environ["DBN"] or "library",
                user=os.environ["DBU"], password=os.environ["DBP"], connect_timeout=3).close()
EOF
}

redis_ok() {  # $1=url
  RURL="$1" "$PY" - <<'EOF' >/dev/null 2>&1
import os, redis
redis.Redis.from_url(os.environ["RURL"], socket_connect_timeout=3).ping()
EOF
}

redis_url_from_pass() {  # codifica la contrasena para la URL
  RP="$1" "$PY" -c 'import os,urllib.parse;print("redis://:"+urllib.parse.quote(os.environ["RP"],safe="")+"@localhost:6379/0")'
}

CANDIDATES=$(ls ~/*/.env ~/*/apps/services/*/.env ~/*/*/.env 2>/dev/null | sort -u)

# ---------------- PostgreSQL ----------------
DB_USER=$(get_var "$ROOT/.env" DB_USER); DB_USER=${DB_USER:-library_user}
DB_PASS=""
for f in "$ROOT/.env" $CANDIDATES; do
  p=$(get_var "$f" DB_PASSWORD); [ -n "$p" ] || continue
  if db_ok "$DB_USER" "$p"; then DB_PASS="$p"; echo "PostgreSQL: credencial valida encontrada en $f"; break; fi
done
while [ -z "$DB_PASS" ]; do
  read -r -s -p "Contrasena de PostgreSQL para $DB_USER (Enter vacio = cancelar): " p; echo
  [ -n "$p" ] || { echo "Cancelado."; exit 1; }
  if db_ok "$DB_USER" "$p"; then DB_PASS="$p"; else echo "  No conecta, intenta de nuevo."; fi
done

# ---------------- Redis ----------------
REDIS_URL=""
for conf in /etc/redis/redis.conf /etc/redis.conf /etc/valkey/valkey.conf; do
  p=$(sudo awk '$1=="requirepass"{print $2; exit}' "$conf" 2>/dev/null | sed -E 's/^"(.*)"$/\1/')
  [ -n "$p" ] || continue
  u=$(redis_url_from_pass "$p")
  if redis_ok "$u"; then REDIS_URL="$u"; echo "Redis: contrasena tomada de $conf"; break; fi
done
if [ -z "$REDIS_URL" ]; then
  for f in $CANDIDATES; do
    u=$(get_var "$f" REDIS_URL); [ -n "$u" ] || continue
    if redis_ok "$u"; then REDIS_URL="$u"; echo "Redis: REDIS_URL valida encontrada en $f"; break; fi
  done
fi
if [ -z "$REDIS_URL" ] && redis_ok "redis://localhost:6379/0"; then
  REDIS_URL="redis://localhost:6379/0"; echo "Redis: no requiere contrasena"
fi
while [ -z "$REDIS_URL" ]; do
  read -r -s -p "Contrasena de Redis (Enter vacio = cancelar): " p; echo
  [ -n "$p" ] || { echo "Cancelado."; exit 1; }
  u=$(redis_url_from_pass "$p")
  if redis_ok "$u"; then REDIS_URL="$u"; else echo "  No conecta, intenta de nuevo."; fi
done

# ---------------- Escribir .env ----------------
set_var() {  # $1=archivo $2=clave $3=valor  (seguro con caracteres especiales)
  local f=$1 k=$2
  [ -f "$f.bak-$STAMP" ] || cp "$f" "$f.bak-$STAMP"
  K="$k" V="$3" awk 'BEGIN{k=ENVIRON["K"]; v=ENVIRON["V"]; done=0}
       index($0, k"=")==1 {print k"="v; done=1; next} {print}
       END{if(!done) print k"="v}' "$f" > "$f.tmp" && cat "$f.tmp" > "$f" && rm -f "$f.tmp"
}

set_var "$ROOT/.env" DB_PASSWORD "$DB_PASS"
for s in login soap users authors pedidos pagos; do
  f="$ROOT/apps/services/$s/.env"
  [ -f "$f" ] || { echo "Falta $f (copialo de ~/ejercicio-redis)"; continue; }
  set_var "$f" REDIS_URL "$REDIS_URL"
  grep -q '^DB_PASSWORD=' "$f" && set_var "$f" DB_PASSWORD "$DB_PASS"
done
chmod 600 "$ROOT/.env" "$ROOT"/apps/services/*/.env
echo ".env actualizados (respaldos *.bak-$STAMP)."

# ---------------- Reiniciar ----------------
./levanta_todo.sh stop
sleep 1
./levanta_todo.sh
