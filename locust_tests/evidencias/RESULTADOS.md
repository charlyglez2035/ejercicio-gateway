# Resultados de las pruebas Locust

Instancia GCP: `34.51.55.6` (zona northamerica-south1-c). Umbral: degradado si p95 > 1000 ms o errores > 5 %.
Los valores se copian de `evidencias/<perfil>_<destino>_stats.csv` (fila `Aggregated`: columnas `Requests/s`, `95%`, `Failure Count / Request Count`).

| Perfil | Destino | Usuarios | Duración | RPS | p95 (ms) | Errores (%) | Umbral | Hipótesis | Conclusión |
|---|---|---|---|---|---|---|---|---|---|
| smoke | monolito | 1 | 30 s | | | | | | |
| smoke | login | 1 | 30 s | | | | | | |
| smoke | books | 1 | 30 s | | | | | | |
| smoke | users | 1 | 30 s | | | | | | |
| smoke | authors | 1 | 30 s | | | | | | |
| smoke | pedidos | 1 | 30 s | | | | | | |
| smoke | pagos | 1 | 30 s | | | | | | |
| load | monolito | 5 | 2 min | | | | | | |
| load | books | 5 | 2 min | | | | | | |
| stress | monolito | 2→20 | 3 min | | | | | | |
| stress | books | 2→20 | 3 min | | | | | | |
| spike | monolito | 2→15→2 | 2 min | | | | | | |
| spike | books | 2→15→2 | 2 min | | | | | | |
| soak | monolito | 3 | 4 min | | | | | | |
| soak | books | 3 | 4 min | | | | | | |
| breakpoint | monolito | 1→20 | ≤4 min | | | | | | |
| breakpoint | books | 1→20 | ≤4 min | | | | | | |

Agrega filas para los demás servicios que ejecutes en load/stress/spike/soak/breakpoint.

## Pruebas no completadas

| Perfil | Destino | Motivo |
|---|---|---|
| | | |
