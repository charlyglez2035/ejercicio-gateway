#!/usr/bin/env bash
# Ejecuta un perfil Locust (headless) contra uno o varios destinos y guarda CSV + HTML.
#
# Uso:   ./run_perfil.sh <perfil> <destino|todos>
#   perfil:  smoke | load | stress | spike | soak | breakpoint
#   destino: monolito | login | books | users | authors | pedidos | pagos | todos
# Ej.:   TARGET_HOST=34.51.55.6 ./run_perfil.sh smoke monolito
#        TARGET_HOST=34.51.55.6 ./run_perfil.sh load todos
#
# "todos" corre cada destino por separado (uno tras otro) para que las
# condiciones sean las mismas y los resultados se puedan comparar.
set -u

cd "$(dirname "$0")"
PERFIL="${1:?Indica el perfil}"
DESTINO="${2:?Indica el destino}"
export PERFIL
export TARGET_HOST="${TARGET_HOST:-34.51.55.6}"
mkdir -p evidencias

declare -A CLASES=(
  [monolito]=MonolitoUser [login]=LoginUser [books]=BooksUser [users]=UsersUser
  [authors]=AuthorsUser [pedidos]=PedidosUser [pagos]=PagosUser
)

if [ "$DESTINO" = "todos" ]; then
  DESTINOS="monolito login books users authors pedidos pagos"
else
  DESTINOS="$DESTINO"
fi

for d in $DESTINOS; do
  clase="${CLASES[$d]:?Destino desconocido: $d}"
  salida="evidencias/${PERFIL}_${d}"
  echo "=== $PERFIL -> $d ($clase) en $TARGET_HOST"
  locust -f locustfile.py --headless --only-summary \
    --csv "$salida" --csv-full-history --html "$salida.html" \
    "$clase" 2>&1 | tee "$salida.log"
  echo "exit code: ${PIPESTATUS[0]} (1 = umbral incumplido o error)"
done
