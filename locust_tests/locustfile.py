"""
Pruebas de rendimiento con Locust: monolito web vs. seis microservicios Flask.

QUE SE MIDE
    Tiempo de respuesta (mediana, p95), solicitudes por segundo (RPS) y
    porcentaje de errores de operaciones de LECTURA publicas (sin JWT, sin
    escrituras) en cada sistema. Cada solicitud se registra con el prefijo
    "[destino]" para que las estadisticas separen monolito y servicios.

DESTINOS Y RUTAS (verificadas en el codigo del repo)
    Clase Locust      Puerto  Rutas
    MonolitoUser      3000    GET /  (redirige a /books), GET /books, GET /books/{isbn}
    LoginUser         5000    GET /health?format=json   (el resto es POST o requiere JWT)
    BooksUser         5001    GET /books, GET /books/{isbn}, GET /health      (format=json)
    UsersUser         5002    GET /roles, GET /health
    AuthorsUser       5003    GET /authors, GET /authors/{id}, GET /authors/{id}/books, GET /health
    PedidosUser       5004    GET /pedidos/estados, GET /stock, GET /stock/{isbn}, GET /health
    PagosUser         5005    GET /pagos/estados, GET /health

    Los puertos son los del README de la raiz y de run_service() de cada app.
    El enunciado lista otros (users 5004, autores 5005, pagos 5002, pedidos
    5003); si tu despliegue usa esos, cambialos con las variables PORT_*.

    Rutas comparables entre arquitecturas:
      [monolito] GET /books         <->  [books] GET /books?format=json
      [monolito] GET /books/{isbn}  <->  [books] GET /books/{isbn}?format=json
    Ojo: el monolito obtiene el listado /books llamando al microservicio books
    (BOOKS_SERVICE_URL), y books cachea en Redis; /books/{isbn} del monolito
    consulta PostgreSQL directamente. Tenlo en cuenta al interpretar.

PARAMETROS
    Usuarios      Usuarios virtuales concurrentes; cada uno ejecuta tareas en bucle.
    spawn_rate    Usuarios que se agregan (o quitan) por segundo hasta llegar al objetivo.
    Duracion      Tiempo total de la ejecucion; al terminar Locust se detiene.
    wait_time     Pausa entre tareas de un mismo usuario: between(1, 3) segundos,
                  simula a una persona que lee la respuesta antes de pedir otra.
                  Con pausa ~2 s, 1 usuario genera ~0.5 RPS; 20 usuarios ~10 RPS.
    Umbral        Degradado si p95 > 1000 ms o errores > 5 % (valor del enunciado).

PERFILES (variable PERFIL; los aplica la clase PerfilCarga)
    smoke       1 usuario, spawn 1/s, 30 s.
    load        5 usuarios, spawn 1/s, 2 min.
    stress      escalones 2 -> 5 -> 10 -> 15 -> 20 usuarios, 36 s cada uno (3 min).
    spike       2 usuarios 40 s, sube a 15 (spawn 15/s) 30 s, baja a 2 hasta 2 min.
    soak        3 usuarios, spawn 1/s, 4 min.
    breakpoint  desde 1 usuario, +1 cada 12 s hasta 20, maximo 4 min. Al incumplir
                el umbral (ventana movil de Locust) se congela la carga y la
                prueba termina 30 s despues; el usuario de quiebre se escribe en
                evidencias/breakpoint_<destino>.txt.

USO (desde locust_tests/, ver run_perfil.sh)
    TARGET_HOST=34.51.55.6 PERFIL=smoke locust -f locustfile.py --headless \
        MonolitoUser --csv evidencias/smoke_monolito --html evidencias/smoke_monolito.html

No se guardan credenciales ni tokens: todas las rutas elegidas son publicas.
"""

import os
from pathlib import Path

from locust import HttpUser, LoadTestShape, between, events, task

# ---------------------------------------------------------------------------
# Configuracion
# ---------------------------------------------------------------------------

TARGET_HOST = os.getenv("TARGET_HOST", "34.51.55.6")
if TARGET_HOST.lower() in ("localhost", "0.0.0.0") or TARGET_HOST.startswith("127."):
    raise SystemExit("TARGET_HOST debe ser la IP de la instancia GCP, no localhost.")

PORTS = {
    "monolito": int(os.getenv("PORT_MONOLITO", "3000")),
    "login": int(os.getenv("PORT_LOGIN", "5000")),
    "books": int(os.getenv("PORT_BOOKS", "5001")),
    "users": int(os.getenv("PORT_USERS", "5002")),
    "authors": int(os.getenv("PORT_AUTHORS", "5003")),
    "pedidos": int(os.getenv("PORT_PEDIDOS", "5004")),
    "pagos": int(os.getenv("PORT_PAGOS", "5005")),
}

# Datos existentes en db/seed.sql (solo se leen).
ISBN = os.getenv("TEST_ISBN", "9780451524935")
AUTHOR_ID = int(os.getenv("TEST_AUTHOR_ID", "1"))

UMBRAL_P95_MS = 1000
UMBRAL_ERRORES = 0.05

PERFIL = os.getenv("PERFIL", "smoke").lower()
EVIDENCIAS = Path(__file__).resolve().parent / "evidencias"


def url(service):
    return f"http://{TARGET_HOST}:{PORTS[service]}"


class ServiceUser(HttpUser):
    abstract = True
    wait_time = between(1, 3)
    service = ""

    def get(self, path, name=None):
        # catch_response permite marcar como fallo respuestas que no son 2xx/3xx
        # o que el monolito devuelve como pagina de error con estado 200.
        label = f"[{self.service}] GET {name or path}"
        with self.client.get(path, name=label, catch_response=True, timeout=10) as resp:
            if resp.status_code >= 400:
                resp.failure(f"HTTP {resp.status_code}")
            else:
                resp.success()


# ---------------------------------------------------------------------------
# Monolito (apps/web-monolito)
# ---------------------------------------------------------------------------

class MonolitoUser(ServiceUser):
    host = url("monolito")
    service = "monolito"

    @task(1)
    def home(self):
        self.get("/")

    @task(3)
    def catalogo(self):
        self.get("/books")

    @task(2)
    def detalle_libro(self):
        self.get(f"/books/{ISBN}", name="/books/{isbn}")


# ---------------------------------------------------------------------------
# Microservicios (apps/services)
# ---------------------------------------------------------------------------

class LoginUser(ServiceUser):
    host = url("login")
    service = "login"

    @task
    def health(self):
        self.get("/health?format=json")


class BooksUser(ServiceUser):
    host = url("books")
    service = "books"

    @task(1)
    def health(self):
        self.get("/health?format=json")

    @task(3)
    def listado(self):
        self.get("/books?format=json")

    @task(2)
    def detalle(self):
        self.get(f"/books/{ISBN}?format=json", name="/books/{isbn}?format=json")


class UsersUser(ServiceUser):
    host = url("users")
    service = "users"

    @task(1)
    def health(self):
        self.get("/health?format=json")

    @task(3)
    def roles(self):
        self.get("/roles?format=json")


class AuthorsUser(ServiceUser):
    host = url("authors")
    service = "authors"

    @task(1)
    def health(self):
        self.get("/health?format=json")

    @task(3)
    def listado(self):
        self.get("/authors?format=json")

    @task(2)
    def detalle(self):
        self.get(f"/authors/{AUTHOR_ID}?format=json", name="/authors/{id}?format=json")

    @task(1)
    def libros_autor(self):
        self.get(f"/authors/{AUTHOR_ID}/books?format=json", name="/authors/{id}/books?format=json")


class PedidosUser(ServiceUser):
    host = url("pedidos")
    service = "pedidos"

    @task(1)
    def health(self):
        self.get("/health?format=json")

    @task(1)
    def estados(self):
        self.get("/pedidos/estados?format=json")

    @task(3)
    def stock(self):
        self.get("/stock?format=json")

    @task(2)
    def stock_libro(self):
        self.get(f"/stock/{ISBN}?format=json", name="/stock/{isbn}?format=json")


class PagosUser(ServiceUser):
    host = url("pagos")
    service = "pagos"

    @task(1)
    def health(self):
        self.get("/health?format=json")

    @task(3)
    def estados(self):
        self.get("/pagos/estados?format=json")


# ---------------------------------------------------------------------------
# Perfiles de carga
# ---------------------------------------------------------------------------

# Cada etapa: (segundo en que termina, usuarios, spawn_rate)
PERFILES = {
    "smoke": [(30, 1, 1)],
    "load": [(120, 5, 1)],
    "stress": [(36, 2, 1), (72, 5, 1), (108, 10, 2), (144, 15, 2), (180, 20, 2)],
    "spike": [(40, 2, 1), (70, 15, 15), (120, 2, 15)],
    "soak": [(240, 3, 1)],
}
BREAKPOINT_PASO_S = 12
BREAKPOINT_MAX_USUARIOS = 20
BREAKPOINT_LIMITE_S = 240
BREAKPOINT_ESPERA_S = 30

if PERFIL not in PERFILES and PERFIL != "breakpoint":
    raise SystemExit(f"PERFIL invalido: {PERFIL}. Usa {', '.join(PERFILES)} o breakpoint.")


class PerfilCarga(LoadTestShape):
    """Controla usuarios y spawn_rate segun PERFIL; reemplaza -u, -r y -t."""

    quiebre = None  # (segundo, usuarios, p95, errores) cuando se incumple el umbral

    def tick(self):
        t = self.get_run_time()
        if PERFIL == "breakpoint":
            return self.tick_breakpoint(t)
        for fin, usuarios, spawn_rate in PERFILES[PERFIL]:
            if t < fin:
                return usuarios, spawn_rate
        return None

    def tick_breakpoint(self, t):
        if self.quiebre:
            segundo, usuarios = self.quiebre[0], self.quiebre[1]
            return (usuarios, 1) if t < segundo + BREAKPOINT_ESPERA_S else None
        if t >= BREAKPOINT_LIMITE_S:
            return None

        usuarios = min(1 + int(t // BREAKPOINT_PASO_S), BREAKPOINT_MAX_USUARIOS)
        total = self.runner.stats.total
        # Se espera a tener muestras suficientes antes de evaluar el umbral.
        if total.num_requests >= 20:
            p95 = total.get_current_response_time_percentile(0.95) or 0
            errores = total.fail_ratio
            if p95 > UMBRAL_P95_MS or errores > UMBRAL_ERRORES:
                actuales = self.runner.user_count
                self.quiebre = (t, actuales, p95, errores)
                registrar_quiebre(self.runner, self.quiebre)
                return actuales, 1
        return usuarios, 1


def registrar_quiebre(runner, quiebre):
    segundo, usuarios, p95, errores = quiebre
    destinos = "_".join(sorted(cls.service for cls in runner.user_classes)) or "destino"
    EVIDENCIAS.mkdir(exist_ok=True)
    texto = (
        f"Umbral incumplido a los {segundo:.0f} s con {usuarios} usuarios: "
        f"p95 ventana actual={p95:.0f} ms, errores acumulados={errores:.2%}\n"
    )
    (EVIDENCIAS / f"breakpoint_{destinos}.txt").write_text(texto, encoding="utf-8")
    print(texto, end="")


@events.quitting.add_listener
def verificar_umbral(environment, **_kwargs):
    """Al terminar, marca la ejecucion como fallida (exit code 1) si se incumplio el umbral."""
    total = environment.stats.total
    if total.num_requests == 0:
        return
    p95 = total.get_response_time_percentile(0.95)
    if p95 > UMBRAL_P95_MS or total.fail_ratio > UMBRAL_ERRORES:
        print(f"UMBRAL INCUMPLIDO: p95={p95:.0f} ms, errores={total.fail_ratio:.2%}")
        environment.process_exit_code = 1
    else:
        print(f"Umbral cumplido: p95={p95:.0f} ms, errores={total.fail_ratio:.2%}")
