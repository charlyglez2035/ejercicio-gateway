#!/usr/bin/env bash
# Evidencia en la VM: muestra el estado de nginx y las peticiones de las 3 apps de escritorio
# en el access log del Reverse Proxy. Cada cliente se identifica por su User-Agent:
#   Java Swing  -> library-java-desktop/1.0
#   PythonTK    -> library-tk-desktop/1.0
#   Electron    -> library-electron-desktop/1.0
# Uso: bash scripts_vm/evidencia_reverse_proxy.sh [lineas]   (por defecto 15 por app)
set -uo pipefail
N="${1:-15}"
LOG=/var/log/nginx/library-gateway.access.log

echo "===== nginx ====="
systemctl is-active nginx
sudo nginx -t 2>&1 | tail -2
echo
echo "===== Puertos: solo :80 es publico; 3000/5000-5005 escuchan en la VM ====="
sudo ss -ltnp | awk 'NR==1 || /:(80|3000|500[0-5]) /'
echo
for app in "Java Swing|library-java-desktop" "PythonTK|library-tk-desktop" "Electron|library-electron-desktop"; do
  name="${app%%|*}"; pattern="${app#*|}"
  echo "===== ${name} (User-Agent ~ ${pattern}) ====="
  sudo grep -E "${pattern}" "$LOG" | tail -n "$N" || echo "(sin peticiones todavia)"
  echo
done
echo "===== Codigos HTTP por cliente (ultimas 500 lineas) ====="
sudo tail -n 500 "$LOG" | awk -F'"' '{split($3,a," "); print a[1], $6}' \
  | grep -E 'library-(java|tk|electron)-desktop' | sort | uniq -c | sort -rn
