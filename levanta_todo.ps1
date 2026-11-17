# Levanta los seis microservicios y el monolito dentro de WSL (el entorno Python/Node del repo vive ahi).
# Uso: .\levanta_todo.ps1        detener: .\levanta_todo.ps1 stop
$repo = (wsl -e wslpath -a ($PSScriptRoot -replace '\\', '/')).Trim()
wsl -e bash -lc "cd '$repo' && ./levanta_todo.sh $($args -join ' ')"
