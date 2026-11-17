#!/usr/bin/env bash
# Instala/actualiza el API Gateway nginx de Library en la VM.
# Uso (desde library/):  bash infra/nginx/install_gateway.sh
# Es idempotente: repetirlo tras cada git pull aplica los cambios del .conf.
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v nginx >/dev/null 2>&1; then
  if command -v dnf >/dev/null 2>&1; then
    sudo dnf install -y nginx
  else
    sudo apt-get update
    sudo apt-get install -y nginx
  fi
fi

sudo install -m 644 library-gateway.conf /etc/nginx/conf.d/library-gateway.conf

# Ubuntu/Debian: el sitio "default" tambien es default_server en :80 y choca con el gateway.
if [ -L /etc/nginx/sites-enabled/default ]; then
  sudo rm /etc/nginx/sites-enabled/default
fi

# CentOS/RHEL con SELinux: nginx necesita permiso para conectarse a los puertos 3000/5000-5005.
if command -v getsebool >/dev/null 2>&1 && [ "$(getenforce 2>/dev/null)" = "Enforcing" ]; then
  getsebool httpd_can_network_connect | grep -q ' on$' || sudo setsebool -P httpd_can_network_connect 1
fi

sudo nginx -t
sudo systemctl enable nginx
if systemctl is-active --quiet nginx; then
  sudo systemctl reload nginx
else
  sudo systemctl start nginx
fi

sleep 1   # el reload es asincrono: espera a que los workers nuevos tomen la config
printf "gateway: "
curl -s http://127.0.0.1/gateway/health || echo "sin respuesta"
echo
