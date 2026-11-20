# Entrega final: apps de escritorio con Reverse Proxy nginx

| Carpeta / archivo en el zip | Contenido |
|---|---|
| `monorepo-library/` | Monorepo completo. Las apps de escritorio están en `apps/Electron_app`, `apps/Python_app` (Tkinter) y `apps/Java_app` (Swing), las tres configuradas para el gateway `http://34.51.105.144` |
| `nginx/` | `library-gateway.conf` (Reverse Proxy / API Gateway) y `install_gateway.sh` (instalación en CentOS con SELinux) |
| `evidencias/` | Capturas y registro de las tres apps a través del gateway (ver `evidencias/README.md`). En `gateway_web/` están las evidencias del gateway con la web |
| `REFLEXION.md` | Cómo fue la implementación, ventajas y desventajas o retos |

Cómo ejecutar cada app:

- **Electron:** `cd apps/Electron_app && npm install && npm start`
- **PythonTK:** `python apps/Python_app/tkinter_client.py`
- **Java:** `apps/Java_app/run.ps1` o `run.sh` (JDK 11+)

Si la IP de la VM cambia, en las tres se actualiza desde la interfaz o con `LIBRARY_GATEWAY_URL`.
