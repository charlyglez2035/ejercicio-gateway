#!/usr/bin/env bash
# Compila y ejecuta el cliente Java (JDK 11+, sin dependencias).
# Uso: ./run.sh [URL_GATEWAY]     por defecto http://34.51.105.144
set -euo pipefail
cd "$(dirname "$0")"
command -v javac >/dev/null || { echo "Instala un JDK 11+ (javac no encontrado)"; exit 1; }
rm -rf out && mkdir -p out
javac --release 11 -encoding UTF-8 -d out $(find src -name '*.java')
jar --create --file library-desktop.jar --main-class library.desktop.LibraryDesktopApp -C out .
exec java -jar library-desktop.jar "$@"
