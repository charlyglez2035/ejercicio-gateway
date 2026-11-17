# API Gateway (nginx)

Un solo punto de entrada en el puerto 80 de la VM. Los microservicios y el monolito
escuchan solo en `127.0.0.1` (lo fuerza `levanta_todo.sh`), así que desde fuera
únicamente se llega a ellos por el gateway.

```text
Electron / Tkinter / Java / navegador
              │  http://<VM_IP>
              ▼
        nginx :80  ── auth_request ──► login GET /session (JWT + Redis)
              │
  /api/books/   → :5001  PUBLICO
  /api/login/   → :5000  publico: login, register, refresh, verify-email, health
  /api/users/   → :5002  JWT
  /api/authors/ → :5003  JWT
  /api/pedidos/ → :5004  JWT (incluye /stock)
  /api/pagos/   → :5005  JWT
  /api/<svc>/health      publico
  /             → :3000  web-monolito (su propia sesion)
```

El prefijo `/api/<svc>` se quita al reenviar: `GET /api/users/users/5` llega al
servicio users como `GET /users/5`. Si no hay token o es inválido, expiró o fue revocado,
el gateway responde `401` sin tocar el servicio; los permisos por rol (`403`)
los sigue decidiendo cada microservicio.

Extras del gateway: rate limit (20 r/s por IP; 5 r/s en login/register/refresh → `429`),
cabecera `X-Request-ID` propagada a los servicios y logs en
`/var/log/nginx/library-gateway.*.log`.

## Despliegue en la VM

```bash
cd ~/ejercicio-redis          # o la carpeta donde clonaste el repo
git pull
cd library                    # si el repo contiene la carpeta library/

# 1. URLs publicas (sustituye <VM_IP>)
#    apps/services/soap/.env   -> MONOLITH_PUBLIC_URL=http://<VM_IP>
#    apps/services/login/.env  -> LOGIN_PUBLIC_URL=http://<VM_IP>/api/login
#    .env (raiz)               -> BOOKS_SERVICE_URL=http://127.0.0.1:5001
#                                 LOGIN_SERVICE_URL=http://127.0.0.1:5000

# 2. Reiniciar servicios (ahora escuchan solo en 127.0.0.1)
./levanta_todo.sh stop
./levanta_todo.sh

# 3. Instalar / actualizar el gateway
bash infra/nginx/install_gateway.sh
```

Al final `levanta_todo.sh` imprime los códigos de respuesta a través del gateway: 200 en todos
los `/health` y 401 en `/api/users/users`.

## Clientes

| Cliente  | Configuración |
|----------|---------------|
| Tkinter  | `.env` del PC: `LOGIN_SERVICE_URL=http://<VM_IP>/api/login`, `BOOKS_SERVICE_URL=http://<VM_IP>/api/books`, `USERS_SERVICE_URL=http://<VM_IP>/api/users`, `AUTHORS_…/api/authors`, `PEDIDOS_…/api/pedidos`, `PAGOS_…/api/pagos` |
| Electron | Endpoint: `http://<VM_IP>/api/books/books` (o `/api/books/books/cards` para imágenes). Si la app guardó una URL vieja, cámbiala con **Guardar URL**. |
| Java u otro | Base `http://<VM_IP>/api/<servicio>`; `POST /api/login/login` → `access_token`; mandar `Authorization: Bearer <token>` |

## Comprobación rápida (desde el PC)

```bash
VM=<VM_IP>
curl -i  http://$VM/gateway/health                               # 200
curl -i "http://$VM/api/books/books?format=json"                 # 200 publico
curl -i "http://$VM/api/users/users?format=json"                 # 401 gateway
TOKEN=$(curl -s -X POST "http://$VM/api/login/login?format=json" \
  -H 'Content-Type: application/json' \
  -d '{"identity":"<usuario>","password":"<password>"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
curl -i "http://$VM/api/users/users?format=json" -H "Authorization: Bearer $TOKEN"   # 200/403 segun rol
curl -i  http://$VM:5002/health                                  # debe fallar: puerto no expuesto
```

## Notas

- La IP externa es efímera: cambia en cada arranque de la VM. nginx no depende de ella,
  pero los clientes y las dos URLs públicas del paso 1 sí.
- Swagger (`/apidocs`) usa rutas absolutas y no funciona detrás del prefijo; consúltalo con
  un túnel SSH: `gcloud compute ssh maquina02 --zone northamerica-south1-c -- -L 5002:127.0.0.1:5002`.
- HTTPS queda pendiente: requiere dominio + certificado (certbot) sobre este mismo `server`.
