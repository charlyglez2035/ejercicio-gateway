#!/usr/bin/env bash
# Instala dependencias Python (.venv) y Node del repo en la VM. Solo hace falta la primera vez.
set -eu

REPO="$(cd "$(dirname "$0")" && pwd)"
[ -d "$REPO/apps/services" ] || REPO="$(dirname "$REPO")"
cd "$REPO"

python3 -m venv .venv
for s in login soap users authors pedidos pagos; do
  .venv/bin/python -m pip install -r "apps/services/$s/requirements.txt"
done
npm install
echo "Dependencias instaladas en $REPO"
