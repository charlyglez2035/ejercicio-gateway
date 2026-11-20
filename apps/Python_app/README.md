# Python_app: microservicio de libros

Microservicio Flask para el catálogo de libros. Expone respuestas JSON y XML, Swagger, CORS, PostgreSQL y el ciclo CRUD solicitado.

## Estructura

- `app.py`: API Flask y vistas HTML.
- `requirements.txt`: dependencias Python.
- `.env.example`: variables necesarias sin credenciales reales.
- `scripts/run_cycle.py`: prueba reproducible `POST -> GET -> PATCH -> GET -> DELETE -> GET`.
- `evidencias/`: resultados JSON y capturas del ejercicio.
- `docs/`: bitácora, reflexión y notas de ejecución.

## Instalación local

Desde la raíz del repositorio:

```bash
cd library
python -m venv .venv
source .venv/bin/activate
pip install -r apps/Python_app/requirements.txt
cp apps/Python_app/.env.example apps/Python_app/.env
```

Configura PostgreSQL en `.env` y ejecuta:

```bash
SERVICE_PORT=5101 python apps/Python_app/app.py
```

Endpoints principales:

- `GET /api/health?format=json`
- `GET /books?format=json`
- `POST /books?format=json`
- `PATCH /books/{isbn}?format=json`
- `PUT /books/{isbn}?format=json` (compatibilidad)
- `DELETE /books/{isbn}?format=json`
- `GET /apidocs/`

## Evidencia del ciclo CRUD

Con el servicio local activo en `http://127.0.0.1:5101`:

```bash
python apps/Python_app/scripts/run_cycle.py
```

El script genera `evidencias/cycle_local.json` y `evidencias/cycle_remote.json`. El ISBN usado es temporal y se elimina al final del ciclo.

## Servicio remoto

La URL documentada para la evidencia remota es `http://34.51.65.146:5001`. Durante la captura del 22 de septiembre de 2026, el host no respondió dentro del timeout; ese resultado está documentado en la bitácora y no se presenta como ejecución exitosa.

## Cliente de escritorio Tkinter detrás del Reverse Proxy

`tkinter_client.py` es la app de escritorio (PythonTK). Ya no usa los puertos 5000-5005: todas sus peticiones pasan por el Reverse Proxy nginx en el puerto 80 de la VM.

```bash
pip install python-dotenv   # opcional
python apps/Python_app/tkinter_client.py
# otra IP:  LIBRARY_GATEWAY_URL=http://<IP_VM> python apps/Python_app/tkinter_client.py
```

Por defecto usa `http://34.51.105.144`. En la pestaña **Login** puedes cambiar la IP en **Gateway** y pulsar **Aplicar gateway**.

| Servicio | URL base en el cliente | Destino interno (nginx) | Acceso en el gateway |
|---|---|---|---|
| Login | `/api/login` | `:5000` | `login`, `register` y `refresh` son públicos; el resto pide JWT |
| Books | `/api/books` | `:5001` | público |
| Users | `/api/users` | `:5002` | JWT |
| Authors | `/api/authors` | `:5003` | JWT |
| Pedidos | `/api/pedidos` | `:5004` | JWT |
| Pagos | `/api/pagos` | `:5005` | JWT |

Después del login, el cliente manda `Authorization: Bearer <JWT>` en todas las rutas protegidas. Con **Omitir JWT** puedes ver el 401 que responde el gateway.

## Seguridad

No subas `.env`, contraseñas, tokens ni contraseñas de aplicación. Usa `.env.example` como plantilla.
