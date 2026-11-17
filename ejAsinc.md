Usando como base el monorepo Library y aplicación de rastreo GPS mediante WebSockets, crea un directorio dentro de apps/JavaDesktop_app

Objetivo: Desarrollar una aplicación de escritorio en Java que funcione como cliente del proyecto Library que hemos estado desarrollando a lo largo del semestre, la aplicación debe consumir e integrar los microservicios disponibles en /apps/services, desplegados en Google Cloud Platform. La aplicación deberá permitir que un usuario se autentique mediante JWT, consulte un catálogo de libros con imágenes y descripciones, agregue productos a una cesta de compras, simule una compra, consulte sus pedidos y realice el seguimiento de una entrega mediante un código de rastreo.

Como parte fundamental del ejercicio, deberán integrar WebSockets para visualizar en un mapa el desplazamiento simulado de un vehículo de reparto por la ciudad de Monterrey, Nuevo León, mostrando en tiempo real la ubicación y el estado de entrega del pedido. El ejercicio deberá demostrar cómo diferentes aplicaciones, servicios, protocolos de comunicación y tecnologías pueden integrarse para construir un sistema informático distribuido.



Escenario:  El proyecto Library cuenta con una arquitectura basada en microservicios que permite administrar usuarios, libros, autores, pedidos y pagos. Se requiere desarrollar una nueva aplicación de escritorio en Java que actúe como cliente de dichos servicios, permitiendo a los usuarios realizar operaciones de comercio electrónico. La aplicación deberá comunicarse con los microservicios existentes mediante sus interfaces REST, sin acceder directamente a sus bases de datos. Adicionalmente, la librería desea ofrecer a sus clientes la posibilidad de consultar el estado de entrega de sus compras. Para ello, se utilizará un sistema de rastreo GPS simulado que transmita información geográfica mediante WebSockets.

El reto consiste en integrar todos estos componentes en una sola aplicación Java, funcional, visualmente atractiva y correctamente estructurada.

El ejercicio deberá seguir la siguiene arquitectura:

Componente	Tecnología
Aplicación cliente	Java
Interfaz gráfica	JavaFX o Swing
Comunicación con microservicios	REST/HTTP
Formato de intercambio	JSON
Autenticación y autorización	JWT
Comunicación en tiempo real	WebSockets
Servidor de rastreo	Flask + Flask-Sock
Visualización geográfica	Leaflet + OpenStreetMap
Infraestructura	Google Cloud Platform
Base de datos	PostgreSQL (mediante servicios)
Comunicación SOAP/XML	Integración demostrativa (si existe endpoint)


Recordar que a aplicación Java deberá ser una aplicación de escritorio, no una página web independiente, no se permitirá establecer conexiones directas desde Java hacia PostgreSQL. Las operaciones de negocio deberán realizarse mediante los microservicios, se deberán utilizar los servicios existentes, sin reimplementarlos innecesariamente, las operaciones protegidas deberán utilizar JWT en todo momento, la conexión WebSocket deberá integrarse en la aplicación Java y no se permitirá simular respuestas REST si existe un microservicio real disponible para la operación.



DESCRIPCIÓN POR MÓDULO

Módulo 1: Autenticación y administración del usuario. Login y Users

Implementar una pantalla de inicio de sesión que se comunique con el microservicio login. La aplicación deberá:

Permitir ingresar correo electrónico y contraseña.
Enviar las credenciales al servicio de autenticación.
Obtener y administrar el token JWT.
Utilizar el token para acceder a las operaciones protegidas de los demás microservicios.
Mostrar el nombre o identificador del usuario autenticado.
Consultar la información de su perfil mediante el servicio users.
Permitir cerrar la sesión.
Detectar tokens expirados o inválidos y solicitar una nueva autenticación.
El JWT deberá enviarse en las solicitudes protegidas mediante el encabezado: Authorization: Bearer <token>. El acceso a funciones administrativas deberá respetar los roles y permisos definidos por los servicios.



Módulo 2. Catálogo de libros. Books y Authors

Desarrollar una interfaz gráfica que permita visualizar los libros disponibles en la librería. Cada libro deberá presentar:

Imagen o portada.
Título.
Autor o autores.
Descripción.
Precio.
ISBN o identificador.
Disponibilidad, cuando el servicio proporcione este dato.
La aplicación deberá permitir:
Consultar el catálogo de libros.
Buscar libros por título.
Visualizar información de sus autores.
Seleccionar un libro para consultar sus detalles.
Agregar libros a la cesta de compras.
Los datos deberán obtenerse mediante los microservicios books y authors. Si alguna imagen no está disponible, se podrá utilizar una imagen genérica identificada como portada no disponible.



Módulo 3. Cesta de compras

Implementar una cesta de compras funcional. El usuario deberá poder:

Agregar uno o varios libros.
Consultar los libros seleccionados.
Modificar cantidades.
Eliminar productos.
Visualizar precios individuales.
Calcular subtotal y total.
Confirmar la intención de compra.
La cesta podrá mantenerse temporalmente en memoria dentro de la aplicación Java. Antes de confirmar una compra, se deberán validar los productos y los precios utilizando la información de los microservicios correspondientes.



Módulo 4. Simulación de compra . Pedidos y Pagos

Una vez confirmada la cesta, la aplicación deberá permitir simular el proceso de compra. El flujo esperado será:

El usuario revisa su cesta.
Confirma la compra.
La aplicación envía la información al microservicio pedidos.
Se genera o recupera el identificador del pedido.
Se realiza una operación de pago simulado utilizando el microservicio pagos.
Se consulta el resultado de la operación.
Se muestra una confirmación de la compra.
Se genera un código único de rastreo asociado al pedido.
No se deberán procesar pagos reales ni solicitar información bancaria auténtica. La aplicación deberá consumir las operaciones reales de pedidos y pagos de acuerdo con sus contratos disponibles. Si estos servicios no proporcionan seguimiento de envíos, dicho seguimiento podrá implementarse como una funcionalidad de simulación independiente, asociada al identificador real del pedido.



Módulo 5. Administración de compras

Crear una sección denominada Mis compras. Esta sección deberá permitir:

Consultar los pedidos del usuario autenticado.
Visualizar los libros incluidos en cada pedido.
Consultar el importe total.
Mostrar el estado de compra y pago.
Visualizar el código de rastreo asignado.
Seleccionar un pedido para consultar sus detalles.
Abrir el seguimiento geográfico del pedido.
Los datos comerciales deberán obtenerse de los servicios pedidos y pagos, sin inventar estados de pago exitoso.



Módulo 6. Rastreo de pedidos mediante WebSockets

Este será uno de los componentes principales de la actividad. Cada compra confirmada deberá contar con un código de rastreo simulado, pero funcional dentro de la aplicación.

Por ejemplo: LIB-MTY-2026-000125

El código deberá identificar un pedido específico y permitir recuperar su seguimiento durante la ejecución de la aplicación.

Requisitos del rastreo:

Al realizar una compra, se deberá generar o asignar un código único de rastreo.
El usuario deberá poder ingresar o seleccionar ese código desde la aplicación Java.
Al seleccionar la opción Rastrear pedido, se deberá abrir una sección de seguimiento con un mapa.
El mapa deberá mostrar la ciudad de Monterrey.
Deberá aparecer un vehículo de reparto asociado al pedido.
El vehículo deberá desplazarse sobre una ruta simulada por calles de Monterrey.
Las coordenadas deberán actualizarse mediante WebSockets.
El movimiento deberá visualizarse sin recargar manualmente el mapa.
La aplicación deberá mostrar la ubicación actual, la ruta recorrida y el estado del pedido.
El cliente deberá poder consultar nuevamente el seguimiento mediante su código.
Los estados de entrega podrán ser:

Pedido confirmado.
Preparando pedido.
Pedido enviado.
En ruta de entrega.
Entregado.
El estado Entregado deberá corresponder al final de la ruta simulada, no simplemente a un cambio arbitrario de texto.



Integración del mapa en Java

Se podrá utilizar JavaFX WebView para incorporar un mapa desarrollado con Leaflet y OpenStreetMap.
El mapa deberá recibir actualizaciones desde el servidor de rastreo mediante WebSockets.
La arquitectura de comunicación esperada será:
Aplicación Java → Microservicios REST → Pedidos y pagos
Simulador GPS → Servidor WebSocket → Mapa integrado en Java
La información de rastreo deberá vincularse con el pedido mediante el código correspondiente.
Se podrá adaptar el proyecto WebSockets proporcionado como referencia para implementar esta funcionalidad.
Importante: el código de rastreo deberá permitir consultar el pedido correcto. No será suficiente mostrar siempre el mismo vehículo o la misma ruta independientemente del código introducido. El servidor deberá asociar cada simulación de entrega con su pedido y evitar que un usuario consulte pedidos ajenos. La autorización del seguimiento deberá vincularse con la sesión autenticada mediante JWT o un mecanismo validado por el backend.



Los endpoints, métodos HTTP y estructuras JSON deberán obtenerse de la documentación o del código de los microservicios disponibles. NO se deberán inventar rutas ni modificar arbitrariamente los contratos establecidos. Si algún servicio carece de una funcionalidad necesaria, se deberá documentar la limitación y desarrollar una adaptación mínima que respete la arquitectura existente.

ENTREGABLES

Se deberá entregar un documento breve que incluya:

A. Explicación de la aplicación

Objetivo de la solución.
Arquitectura de componentes.
Tecnologías utilizadas.
Microservicios consumidos y operaciones realizadas.
Proceso de autenticación mediante JWT.
Funcionamiento de la cesta y simulación de compra.
Generación y utilización del código de rastreo.
Funcionamiento del sistema de seguimiento mediante WebSockets.
Evidencias del funcionamiento de la aplicación
B. Reflexión sobre tecnologías de integración

Responder razonadamente las siguientes preguntas:

REST: ¿Por qué resulta conveniente utilizar REST para consultar libros, autenticar usuarios, administrar pedidos y registrar pagos? ¿Qué ventajas proporciona frente a integrar directamente las aplicaciones con la base de datos?

WebSockets: ¿Por qué WebSockets es una opción adecuada para visualizar el movimiento de un vehículo en tiempo real? ¿Qué diferencias existen respecto de consultar continuamente un endpoint REST?

SOAP: ¿Qué diferencias existen entre REST y SOAP? ¿En qué escenarios empresariales podría ser conveniente utilizar SOAP y XML en lugar de una interfaz REST/JSON? ¿Cómo podría integrarse un servicio SOAP con una aplicación Java?

Google Cloud Platform (GCP): ¿Qué ventajas ofrece ejecutar los microservicios en una instancia de GCP? ¿Qué implicaciones tiene la comunicación entre la aplicación Java local y los servicios alojados en la nube, particularmente respecto a disponibilidad, seguridad, latencia y escalabilidad?

PostgreSQL: ¿Cuál es la función de PostgreSQL en la arquitectura Library? ¿Por qué debe ser responsabilidad de los servicios acceder a la base de datos en lugar de permitir consultas SQL directas desde la aplicación Java?

Arquitecturas de comunicación: ¿Qué diferencias identificaron entre la comunicación síncrona mediante REST, la comunicación persistente mediante WebSockets y los contratos estructurados de SOAP? ¿Qué protocolo utilizarían para autenticación, consulta de catálogos, pagos y rastreo GPS, y por qué?

C. Reflexión sobre el uso de inteligencia artificial

Se permite utilizar herramientas de IA para generar código, explicar errores, diseñar interfaces, crear clientes REST y apoyar la implementación de WebSockets. Sin embargo, cada quien deberá explicar:

¿Qué herramienta de IA utilizó?
¿En qué actividades se apoyó en la IA?
¿Qué instrucciones o prompts fueron más útiles?
¿Qué errores o limitaciones presentó el código generado?
¿Qué modificaciones realizó el equipo para que funcionara correctamente?
¿Qué conceptos de integración comprendió mejor durante la práctica?
¿Qué partes del proyecto podría desarrollar sin asistencia de IA?
La reflexión deberá demostrar comprensión técnica. El uso de IA no sustituye la responsabilidad del cada uno de verificar, explicar y probar el código generado.

D. Monorepo compactado en formato .zp o tar.gz