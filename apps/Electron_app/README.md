# Library Catalog para Windows 11

Aplicacion de escritorio construida con Electron y organizada con el patron MVC. Consume exclusivamente XML del microservicio de libros, muestra el catalogo con cards, imagenes, paginacion y carga bajo demanda.

## Arquitectura MVC

- `main/models/xmlCatalogModel.js`: modelo del proceso principal; valida la URL y descarga exclusivamente XML.
- `renderer/models/bookModel.js`: modelo de dominio; convierte el XML en libros y persiste el endpoint con `localStorage`.
- `renderer/views/catalogView.js`: vista; actualiza el DOM, cards, estados y controles de paginacion.
- `renderer/controllers/catalogController.js`: controlador; coordina eventos, peticiones, errores y cambio de pagina.
- `renderer/app.js`: punto de composicion que instancia y conecta modelo, vista y controlador.
- `main.js` y `preload.js`: infraestructura Electron e interfaz IPC segura.

## Requisitos

- Windows 11
- Node.js 20 o superior
- Acceso de red al endpoint XML

## Instalacion y ejecucion

Desde esta carpeta (`library/apps/Electron_app`), abre PowerShell y ejecuta:

```powershell
npm install
npm start
```

Desde Git Bash, WSL o Linux tambien puedes usar el script incluido:

```bash
chmod +x run.sh
./run.sh
```

El script verifica Node.js y npm, instala las dependencias si `node_modules` no existe y abre la aplicacion.

Tambien puedes ejecutar los comandos usando la ruta completa:

```powershell
Set-Location "C:\ruta\a\integracion02\library\apps\Electron_app"
npm install
npm start
```

`npm install` descarga las dependencias. `npm start` abre la ventana de Electron en modo desarrollo.

## Reverse Proxy (nginx)

La app ya no llama al microservicio por su puerto (`:5001`): todo pasa por el Reverse Proxy / API Gateway nginx en el puerto 80 de la VM.

| Endpoint en la app | nginx lo envía a |
|---|---|
| `http://34.51.105.144/api/books/books` | books `127.0.0.1:5001/books` |
| `http://34.51.105.144/api/books/books/cards` | books `127.0.0.1:5001/books/cards` |

La URL inicial es `http://34.51.105.144/api/books/books`. Como la IP de la VM es efímera, se puede cambiar en **Endpoint de libros** y guardar con **Guardar URL**; queda persistida en `localStorage`. Books es público en el gateway, por lo que no requiere JWT.

El endpoint `/books/cards` devuelve las imágenes en XML como `images/image/image_url`. En el servidor Flask, `MONOLITH_PUBLIC_URL` debe apuntar a la URL pública detrás del proxy, por ejemplo `http://34.51.105.144/`; no debe quedarse en `localhost` si Electron se ejecuta en otro equipo. Si un libro no tiene imagen en el XML, la aplicación muestra automáticamente una portada de stock.

## Uso

- **Actualizar catálogo** solicita el XML configurado y vuelve a dibujar las cards.
- Las flechas inferiores cambian de página; cada página muestra hasta 8 libros.
- La app rechaza respuestas JSON y valida el XML antes de renderizarlo.
- Las imágenes se cargan desde las URL incluidas en el XML; si faltan, se muestra una portada de stock. Si una imagen falla, se muestra el título como alternativa.

## Crear instalador `.exe`

```powershell
npm run build
```

El instalador para Windows se genera en `dist/`. Para probar el modo empaquetado sin publicar, también puedes ejecutar:

```powershell
npx electron-builder --win --dir
```

## Formato XML esperado

El endpoint debe devolver una colección con nodos `book` o `Book` y campos `isbn`, `title`, `authors`, `publication_year`, `category` o `genre`, `price` y `stock`. También se aceptan `Authors/Author`, `PublicationYear` e `Images/Image/URL`; los nombres de las etiquetas no distinguen mayúsculas y minúsculas.
