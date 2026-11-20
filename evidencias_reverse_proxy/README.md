# Evidencias: apps de escritorio detrás del Reverse Proxy nginx

- **Gateway:** `http://34.51.105.144` (nginx en el puerto 80 de `maquina02`, GCP).
- **Tráfico:** Electron, PythonTK y Java usan solo esta URL; el firewall solo deja entrar por el 80.
- **Fecha y versión:** capturas del 20/11/2026, con el commit `3b14fee` desplegado.

| # | Archivo | Qué demuestra |
|---|---|---|
| **Infraestructura** | | |
| 01 | `01_vm_nginx_activo_config_y_puertos.png` | nginx activo en la VM; la config instalada en `/etc/nginx/conf.d/` es idéntica a la del repo; nginx escucha en el :80 y los servicios en 3000/5000-5005 dentro de la VM; `/gateway/health` responde con `X-Request-ID` |
| 02 | `02_nginx_config_library_gateway.png` | Archivo de configuración `library-gateway.conf`: upstreams, rutas `/api/<servicio>/`, `auth_request` con JWT, `limit_req` y health |
| 03 | `03_firewall_solo_puerto_80.png` | Reglas de firewall de GCP. Desde la PC, 3000 y 5000-5005 están CERRADOS y el 80 está ABIERTO; por el gateway, books da 200 y authors sin JWT da 401 |
| **Electron** | | |
| 04 | `04_electron_catalogo_via_gateway.png` | Catálogo XML cargado desde `http://34.51.105.144/api/books/books` |
| 05 | `05_electron_cards_via_gateway.png` | Endpoint `/api/books/books/cards`, también por el gateway |
| **PythonTK** | | |
| 06 | `06_tk_gateway_semaforos.png` | Campo **Gateway** y los seis servicios bajo `/api/<servicio>` con su semáforo en verde |
| 07 | `07_tk_books_publico_via_gateway.png` | Books (público) sin sesión |
| 08 | `08_tk_login_ok.png` | Login por `/api/login/login`; el JWT queda en memoria |
| 09 | `09_tk_authors_con_jwt.png` | Authors (ruta protegida) con `Authorization: Bearer` |
| 10 | `10_tk_401_sin_jwt.png` | Con **Omitir JWT**, Pedidos devuelve el 401 del gateway |
| **Java Swing** | | |
| 11 | `11_java_health_via_gateway.png` | `/gateway/health` y los seis servicios en OK; el log muestra el `X-Request-ID` |
| 12 | `12_java_libros_publico.png` | Pestaña Libros (pública) |
| 13 | `13_java_login_ok.png` | Login a través del Reverse Proxy |
| 14 | `14_java_autores_con_jwt.png` | Autores con JWT (`JWT=si` en el log) |
| 15 | `15_java_sesion_con_jwt.png` | `/api/login/session` con JWT |
| 16 | `16_java_401_sin_jwt.png` | Con **Omitir JWT**, 401 del gateway (`JWT=no`) |
| **Registro de nginx** | | |
| 17 | `17_nginx_access_log_3_apps.png` | Access log de nginx: peticiones de las tres apps separadas por User-Agent, con 200 y 401 |
| 18 | `18_registro_nginx_3_apps.txt` | El mismo registro en texto (`scripts_vm/evidencia_reverse_proxy.sh`) |

En la carpeta `anexo_gateway_web/` están las capturas de la entrega anterior del gateway con la web (IP de esa sesión: 34.51.115.157).

Usuario de las capturas: `evidencia_desktop` (rol `customer`), creado solo para estas evidencias.
