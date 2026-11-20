# Compila y ejecuta el cliente Java (JDK 11+, sin dependencias).
# Uso: .\run.ps1 [URL_GATEWAY]     por defecto http://34.51.105.144
param([string]$Gateway = "")
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) { throw "Instala un JDK 11+ (javac no encontrado)" }
if (Test-Path out) { Remove-Item -Recurse -Force out }
New-Item -ItemType Directory out | Out-Null
$sources = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
javac --release 11 -encoding UTF-8 -d out $sources
jar --create --file library-desktop.jar --main-class library.desktop.LibraryDesktopApp -C out .
if ($Gateway) { java -jar library-desktop.jar $Gateway } else { java -jar library-desktop.jar }
