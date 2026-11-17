# Pruebas Locust

Compara el monolito (`apps/web-monolito`, puerto 3000) con los microservicios Flask (`apps/services`, 5000–5005) usando solo lecturas públicas. Rutas, parámetros, perfiles y umbral están explicados en el docstring de [locustfile.py](locustfile.py).

## Preparación

1. En la VM GCP (`34.51.55.6`): PostgreSQL + Redis activos, datos cargados según el README raíz, y `./levanta_todo.sh`.
2. Firewall de GCP: permitir TCP 3000 y 5000-5005 hacia la VM desde tu IP.
3. Locust (ya instalado en WSL: `~/.venvs/locust`, `locust --version` → 2.47.0).

## Ejecución (desde WSL, en esta carpeta)

```bash
chmod +x run_perfil.sh
export TARGET_HOST=34.51.55.6
# Smoke manual previo a la carga:
curl "http://$TARGET_HOST:5001/health?format=json"
./run_perfil.sh smoke todos
./run_perfil.sh load monolito;       ./run_perfil.sh load books
./run_perfil.sh stress monolito;     ./run_perfil.sh stress books
./run_perfil.sh spike monolito;      ./run_perfil.sh spike books
./run_perfil.sh soak monolito;       ./run_perfil.sh soak books
./run_perfil.sh breakpoint monolito; ./run_perfil.sh breakpoint books
```

Cada ejecución deja en `evidencias/`: `<perfil>_<destino>_stats.csv`, `_stats_history.csv`, `_failures.csv`, `_exceptions.csv`, `.html` y `.log`. El perfil `breakpoint` además escribe `breakpoint_<destino>.txt` si se incumple el umbral.

Para usar la interfaz web (capturas): `PERFIL=load locust -f locustfile.py MonolitoUser` y abrir http://localhost:8089.

Si los puertos de tu despliegue son los del enunciado (users 5004, autores 5005, pagos 5002, pedidos 5003), expórtalos: `export PORT_USERS=5004 PORT_AUTHORS=5005 PORT_PAGOS=5002 PORT_PEDIDOS=5003`.
