# Reflexión: aplicaciones de escritorio detrás de un Reverse Proxy (nginx)

## Cómo fue la implementación

El punto de partida fue Library: un monolito web (Node, puerto 3000) y seis microservicios Flask (login 5000, books 5001, users 5002, authors 5003, pedidos 5004 y pagos 5005) en la VM `maquina02` de GCP. Antes, cada app de escritorio apuntaba directo a un puerto y a una IP fija. El cliente Tkinter tenía seis URL, una por servicio, y Electron llamaba a `:5001/books`.

1. **Gateway único.** Instalé nginx en la VM como Reverse Proxy y API Gateway en el puerto 80, con `infra/nginx/library-gateway.conf` y `install_gateway.sh`. Cada microservicio quedó publicado bajo un prefijo `/api/<servicio>/`, nginx quita ese prefijo al reenviar la petición, y `/` va al monolito. Las apps de escritorio ya no usan los puertos 3000 y 5000-5005.
2. **Seguridad en el borde.** Las rutas protegidas usan `auth_request`. Antes de reenviar, nginx consulta `login /session` para validar la firma, la expiración, la revocación y la sesión en Redis. Si el JWT no es válido, nginx responde `401` en JSON y el microservicio ni se entera. También agregué `limit_req` (20 r/s para la API y 5 r/s para el login), `X-Request-ID` en cada petición y un health check propio en `/gateway/health`.
3. **Adaptación de las tres apps de escritorio.** Las tres reciben ahora una sola URL base, la del gateway (`http://34.51.105.144` por defecto), y arman las rutas `/api/...`:
   - **Electron:** el endpoint XML pasó a `http://<IP>/api/books/books`. Cambié la clave de `localStorage` para que no siga usando endpoints viejos guardados con puertos.
   - **PythonTK:** las seis variables `*_SERVICE_URL` se reemplazaron por `LIBRARY_GATEWAY_URL`. Agregué un campo **Gateway** y un botón **Aplicar** que recalcula todas las URL. Con sesión iniciada, el JWT se manda también en consultas que antes eran públicas, porque detrás del gateway solo books y login lo son.
   - **Java (Swing), nueva:** escrita con JDK puro (`java.net.http`). Incluye login, catálogo, rutas protegidas, health check y un log que muestra el `X-Request-ID` de nginx.
   - Cada app manda un `User-Agent` propio (`library-electron-desktop`, `library-tk-desktop`, `library-java-desktop`). Así, en el access log de nginx se ve qué cliente hizo cada petición (`scripts_vm/evidencia_reverse_proxy.sh`).
4. **Despliegue.** Los cambios se hacen en local, se suben a GitHub y la VM los baja con `git pull`. No se edita nada a mano en el servidor.

## Ventajas identificadas

- **Un solo punto de entrada.** Las apps conocen una sola URL y un solo puerto. Si cambia la IP efímera de la VM, se actualiza en un campo en lugar de seis. Si un servicio cambia de puerto, solo se toca nginx y los clientes no se recompilan.
- **Menos superficie expuesta.** Los clientes solo necesitan el puerto 80. Al final borré las reglas de firewall de ejercicios anteriores que abrían 3000 y 5000-5005. Ahora los microservicios solo se alcanzan a través de nginx: los puertos directos aparecen cerrados desde fuera (evidencia 03).
- **Seguridad centralizada.** La validación del JWT y el límite de peticiones se aplican igual a todos los clientes (web, Electron, Tkinter y Java), sin repetir lógica en cada uno. Los microservicios siguen validando JWT y rol, así que hay defensa en profundidad.
- **Trazabilidad.** El `X-Request-ID` aparece en la respuesta, en el log de la app y en el log de nginx, y con eso se sigue una petición de punta a punta. El User-Agent indica qué app la hizo.
- **Clientes más simples.** Las tres apps comparten el mismo contrato de rutas (`/api/<servicio>/...`), sin importar el lenguaje.

## Desventajas y retos encontrados

- **Punto único de falla.** Si nginx se cae, las tres apps y la web se quedan sin servicio aunque los microservicios sigan funcionando. Lo vimos cuando nginx respondía `502 Bad Gateway` porque los servicios Flask todavía no arrancaban.
- **IP efímera.** La IP externa de la VM cambió entre sesiones (34.51.65.146, 34.51.115.157 y luego 34.51.105.144). Por eso las tres apps permiten cambiar la URL y Electron usa una clave nueva de `localStorage`. Lo ideal sería una IP estática o un dominio.
- **Reescritura de rutas.** Con `proxy_pass` con barra final, nginx quita el prefijo, y eso genera URL poco obvias, como `/api/books/api/books` en Tkinter. Hay que entender bien cómo se combinan `location` y `proxy_pass` para no romper rutas.
- **Rutas que dejaron de ser públicas.** Consultas que antes eran públicas en los servicios (roles, estados de pedido, stock) ahora pasan por `auth_request`. Hubo que hacer que los clientes manden el JWT siempre que haya sesión.
- **Solo HTTP.** El gateway trabaja por HTTP en el puerto 80, así que el JWT viaja sin cifrar. Para producción hace falta TLS (Let's Encrypt), y eso requiere un dominio.
- **Entorno de la VM.** CentOS Stream 10 con SELinux en modo enforcing bloqueaba las conexiones de nginx a los upstreams hasta activar `httpd_can_network_connect`. También hubo que cuidar el choque con el `server_name _` del `nginx.conf` por defecto.
- **Electron y sus restricciones.** La CSP de Electron solo permitía imágenes por `https:`, y las portadas servidas por HTTP a través del proxy no cargaban. Hubo que permitir `http:` en `img-src`.

## Conclusión

El Reverse Proxy cambió la forma de conectar las apps de escritorio: ya no conocen cada microservicio, solo hablan con un gateway que enruta, autentica, limita y registra. El costo es depender de un componente central y tener que diseñar bien sus rutas. A cambio, los clientes quedaron más simples, más seguros y más fáciles de mantener.
