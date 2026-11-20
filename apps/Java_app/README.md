# Java_app: cliente de escritorio Swing detrás del Reverse Proxy

Cliente de escritorio en Java (Swing + `java.net.http.HttpClient`) para Library. No tiene dependencias externas: solo necesita un JDK 11 o superior.

La app nunca llama a los puertos internos de los microservicios (5000-5005). Todas las peticiones van al **Reverse Proxy / API Gateway nginx** en el puerto 80 de la VM. nginx decide a qué servicio enviarlas y, en las rutas protegidas, revisa el JWT con `auth_request`.

## Estructura

| Archivo | Responsabilidad |
|---|---|
| `src/library/desktop/LibraryDesktopApp.java` | Ventana Swing: gateway, login, pestañas, tabla, detalle y log |
| `src/library/desktop/GatewayClient.java` | Cliente HTTP. Arma las URL `<gateway>/api/...`, agrega `Authorization: Bearer` y mide tiempos |
| `src/library/desktop/Json.java` | Parser JSON mínimo, sin librerías |
| `run.ps1` / `run.sh` | Compilan a `out/`, generan `library-desktop.jar` y lo ejecutan |

## Ejecución

```powershell
# Windows (PowerShell)
cd library\apps\Java_app
.\run.ps1                         # usa http://34.51.105.144
.\run.ps1 http://<IP_VM>          # si cambió la IP efímera
```

```bash
# Linux / WSL
cd library/apps/Java_app
./run.sh                          # o ./run.sh http://<IP_VM>
```

La URL del gateway también se puede definir con la variable `LIBRARY_GATEWAY_URL` o cambiar en la propia app con **Aplicar**.

## Rutas que consume

| Pestaña / acción | Petición al gateway | nginx la envía a | Acceso |
|---|---|---|---|
| Comprobar gateway | `GET /gateway/health` y `/api/<svc>/health` | nginx y cada servicio | público |
| Libros | `GET /api/books/books` | books `:5001/books` | público |
| Login | `POST /api/login/login` | login `:5000/login` | público (con `limit_req`) |
| Logout | `POST /api/login/logout` | login `:5000/logout` | JWT |
| Sesión | `GET /api/login/session` | login `:5000/session` | JWT |
| Autores | `GET /api/authors/authors` | authors `:5003` | JWT |
| Pedidos | `GET /api/pedidos/pedidos` | pedidos `:5004` | JWT |
| Pagos | `GET /api/pagos/pagos` | pagos `:5005` | JWT |

## Qué muestra para las evidencias

- El panel inferior registra cada petición con método, URL del gateway, código HTTP, el `X-Request-ID` que agrega nginx, el tiempo en ms y si se envió el JWT. Ese `X-Request-ID` también aparece en `/var/log/nginx/library-gateway.access.log`.
- **Omitir JWT** sirve para mostrar el `401` que devuelve el gateway sin que la petición llegue al microservicio.
- Los códigos `429` (`limit_req`) y `502`/`504` (servicio caído detrás de nginx) se muestran con mensajes claros.
