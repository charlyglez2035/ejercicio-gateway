Objetivo: Diseñar y ejecutar pruebas de rendimiento con Locust para comparar el monolito web y la arquitectura de microservicios de este monorepo. Al finalizar, deberás explicar qué mide cada prueba, qué significan sus parámetros y qué conclusiones permiten (y no permiten) obtener sus resultados. La IA puede ayudarte a entender Locust, proponer código y diagnosticar errores. No sustituye tus hipótesis, la verificación de las rutas ni la interpretación de las métricas: debes poder explicar y defender tu trabajo.

El monolito esapps/web-monolito, servido en http://ip_instanciagcp:3000. Los seis microservicios Flask se sirven en:

Servicio URL base
 
login http://ip_instanciagcp:5000
books http://ip_instanciagcp:5001
pagos http://ip_instanciagcp:5002
pedidos http://ip_instanciagcp:5003
users http://ip_instanciagcp:5004
autores http://ip_instanciagcp:5005

Identifica las rutas disponibles en los OpenAPI deservices/y en el código del monolito. Selecciona solicitudes representativas y, cuando sea posible, comparables. Prefiere operaciones de lectura que no cambien datos. Para smoke puedes comprobarGET /health?format=jsonen cada microservicio yGET /en el monolito; una respuesta de salud confirma disponibilidad, pero no demuestra que las operaciones de negocio funcionen.
INSTRUCCIONES
1. Trabaja desde una copia local del monorepo y confirma que PostgreSQL y sus datos estén preparados según elREADME.mdde la raíz.
2. En PowerShell, inicia los seis microservicios: levanta_todo.ps1  Comprueba que respondan en sus puertos y que el estado de salud.
3. Inicia el monolito desde apps\web-monolito.   Si faltan dependencias, instala las indicadas por elpackage.jsonde esa aplicación. Abrehttp://localhost:3000y confirma que responde.
4. Instala Locust en el entorno de Python que usarás para este ejercicio  y compruebalocust --version.
5. Crea uan carpeta en el tu monorepo llamadad locust_tests, y generaa un archivo locustfile.py. Debe permitir probar el monolito y los seis microservicios;
6. Registra nombres de solicitudes que permitan distinguir cada sistema o servicio en las estadísticas. Usa autenticación solo cuando una ruta seleccionada la requiera y no guardes contraseñas ni tokens en el archivo entregado.

Si un componente no inicia, registra el error y continúa con los componentes disponibles. No inventes resultados ni presentes una prueba fallida como exitosa.
PRUEBAS LOCUST REQUERIDAS

Antes de cada ejecución, anota qué esperas observar. Mantén constantes las rutas y condiciones al comparar los sistemas. Como referencia para esta práctica, considera degradado el servicio si el p95 supera 1 segundo o los errores superan 5 %; justifica cualquier umbral distinto antes de ejecutar.

Smoke Confirmar que el escenario y las rutas responden correctamente con carga mínima. 1 usuario, 30 segundos.
Load Observar el comportamiento con una carga esperada y estable. 5 usuarios, incremento de 1 usuario/segundo, 2 minutos.
Stress Aumentar gradualmente la carga por encima de la esperada y observar la degradación. Rampa por etapas de 2 a 20 usuarios, hasta 3 minutos.
Spike Medir el efecto de un aumento súbito y la recuperación posterior. Subir de 2 a 15 usuarios rápidamente, mantener brevemente y volver a 2; 2 minutos.
Soak Buscar degradación durante una carga sostenida. 3 usuarios durante 4 minutos. Es una miniatura didáctica: un soak real suele durar horas.
Punto de quiebre Estimar cuándo se incumple el umbral de servicio definido. Incrementar desde 1 usuario, con máximo de 20 y límite de 4 minutos; detener el aumento al incumplir el umbral.

Los valores son límites iniciales para el equipo local, no metas universales. Si el equipo se satura, reduce la concurrencia y anótalo. Define y explica en tu archivo los usuarios, spawn_rate, duración, pausas entre tareas y umbral. No confundas el punto de quiebre observado en una prueba corta con la capacidad máxima real del sistema.

USO DE IA
- Antes de pedir código, escribe qué quieres medir y qué rutas vas a usar.
- Pide a la IA que explique User, @task, wait_time, usuarios, spawn_rate, duración, estadísticas y perfiles de carga.
- Revisa el código generado, valida rutas y métodos HTTP, evita escrituras repetidas y confirma que las métricas separen cada destino.
- Ejecuta al menos una consulta de smoke manualmente antes de generar carga.
- Conserva en la reflexión un ejemplo de una sugerencia útil de IA y una corrección o verificación que hayas tenido que hacer.
- No envíes credenciales, secretos, datos personales ni archivos .env a la IA ni los incluyas en la entrega.

SEGURIDAD Y LÍMITES

Ejecuta las pruebas en tu instancia GCP. No apuntes Locust a localhost.

EVIDENCIAS Y REFLEXIÓN

Incluye en locust_tests/evidencias/:

*  Los CSV de cada ejecución, generados con la opción --csv de Locust, y un reporte HTML por perfil con --html cuando sea viable.
*  Capturas de la interfaz o de las estadísticas relevantes, si las utilizaste.
*  Un RESULTADOS.md con una fila por prueba y destino que incluya perfil, usuarios, duración, solicitudes/segundo (RPS), p95, porcentaje de errores, resultado del umbral, hipótesis y conclusión breve. Indica explícitamente las pruebas que no se pudieron completar y por qué.
*  Un archivo llamadoREFLEXION.md de aproximadamente una página que responda:
  1. ¿Qué diferencias observaste entre el monolito y los microservicios? ¿Qué evidencia sustenta la comparación?
  2. ¿Qué servicio o ruta mostró primero degradación y cómo lo determinaste?
  3. ¿Qué diferencias hubo entre load, stress, spike, soak y punto de quiebre?
  4. ¿Qué limitaciones tuvo la prueba (duración, datos, máquina, red, rutas o generación de carga)?
  5. ¿Qué sugerencia de IA te ayudó, qué tuviste que corregir o comprobar y qué aprendiste por ti mismo?
  6. ¿Qué cambiarías en la arquitectura o en la siguiente prueba? Justifica la propuesta.
  7. ¿Qué indican las gráficas? explica detalladamente

No inventes valores de p95, RPS, errores ni resultados. Si una métrica no está disponible, indícalo y explica por qué.