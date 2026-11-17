#!/usr/bin/env bash
# Instala/actualiza el API Gateway nginx de Library en la VM.
# Uso (desde library/):  bash infra/nginx/install_gateway.sh
# Es idempotente: repetirlo tras cada git pull aplica los cambios del .conf.
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v nginx >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y nginx
fi

sudo cp library-gateway.conf /etc/nginx/conf.d/library-gateway.conf

# El sitio "default" de Ubuntu tambien escucha en :80 como default_server y choca con el gateway.
if [ -L /etc/nginx/sites-enabled/default ]; then
  sudo rm /etc/nginx/sites-enabled/default
fi

sudo nginx -t
sudo systemctl enable nginx
if systemctl is-active --quiet nginx; then
  sudo systemctl reload nginx
else
  sudo systemctl start nginx
fi

printf "gateway: "
curl -s http://127.0.0.1/gateway/health || echo "sin respuesta"
echo
