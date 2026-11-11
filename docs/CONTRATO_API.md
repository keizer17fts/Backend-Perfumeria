# Contrato de la API v2 (backend real, etapa 15)

Reemplaza a `contrato-api-frontend.md` (que describía json-server). El front sigue llamando a **una sola URL**:
`VITE_API_URL=http://localhost:8080/api` (el gateway, etapa 17). Mientras no exista el gateway, `ms-catalogo` se prueba directo en `http://localhost:8081/api`.

## Decisiones
- **MySQL 8**, una BD por microservicio (`luxury_catalogo`, `luxury_usuarios`, `luxury_ordenes`). No hay claves foráneas entre BD distintas: `ordenes.usuarioId` y `items[].productoId` son referencias lógicas; la orden guarda una copia de los datos del producto.
- **Spring Boot 3 (Java 21) + JPA/Hibernate + Bean Validation.**
- Todas las rutas empiezan con `/api` dentro de cada servicio, así el gateway solo reenvía sin reescribir.
- **ids como texto** en el JSON (`"id": "7"`), igual que el mock, para no tocar el front.
- Los campos `null` no se envían (forma idéntica a json-server).
- Errores: `{ "status": 409, "error": "Conflict", "mensaje": "..." }`. El front solo lee `status`.

## Códigos de estado
| Código | Cuándo |
|---|---|
| 200 / 201 / 204 | OK / creado (con `Location`) / borrado sin cuerpo |
| 400 | validación (precio negativo, campo obligatorio, categoría u producto inexistente, JSON mal formado) |
| 404 | el recurso no existe |
| 409 | duplicado (código de producto, id de categoría) o regla de negocio (categoría con productos) |

## ms-catalogo (implementado) · puerto 8081

### Productos `/api/productos`
| Método | Ruta | Detalle |
|---|---|---|
| GET | `/api/productos` | Filtros por igualdad (sin distinguir mayúsculas): `categoria, genero, familia, estacion, tipo, marca, concentracion, codigo`. Búsqueda `q` en nombre/marca. Orden por id. |
| GET | `/api/productos/{id}` | 404 si no existe |
| POST | `/api/productos` | Obligatorios: `codigo` (único), `marca`, `nombre`, `precio ≥ 0`. `stock ≥ 0`. `categoria` debe existir. Ignora `id` del cuerpo. |
| PUT | `/api/productos/{id}` | Reemplaza el producto completo |
| PATCH | `/api/productos/{id}` | Solo los campos enviados (ej. `{"stock": 5}`) |
| DELETE | `/api/productos/{id}` | Borra también sus ofertas |

Campos: `id, codigo, ml, marca, nombre, precio, stock, stockCritico, categoria, familia, estacion, genero, tipo, concentracion, imagen, descripcion, heroFondo, historiaTitulo, historia, historiaImagen, ctaFondo, fondoPagina, ctaTitulo, ctaTexto, navClaro, heroAjuste, humor[], maridaje[], notas{salida[],corazon[],fondo[]}, notasImagenes{}, inspiradoEn{marca,nombre,precioOriginal}, colores{}`.

### Categorías `/api/categorias`
`GET` lista (por nombre) · `GET /{id}` · `POST` (si no viene `id`, se genera el slug desde `nombre`; 409 si ya existe) · `PUT /{id}` · `DELETE /{id}` (**409** si tiene productos).
Campos: `id (slug), nombre, descripcion, imagen`.

### Ofertas `/api/ofertas`
`GET ?activa=true&_expand=producto` (misma forma que usa `ofertasService.listarActivas`: cada oferta con su `producto` dentro) · `GET /{id}` · `POST` · `PUT /{id}` · `DELETE /{id}`.
Campos: `id, productoId, descuento (1-90, entero), etiqueta, vigenteHasta (yyyy-MM-dd), activa`. `productoId` debe existir (400 si no).
El precio final **no** se guarda: `Math.round(precio * (1 - descuento / 100))` en el front.

### Salud
`GET /api/catalogo/salud` → `{ "servicio": "ms-catalogo", "estado": "OK" }`

## ms-usuarios (implementado, etapa 16) · puerto 8082

Base `luxury_usuarios`. **La contraseña nunca sale en ninguna respuesta** y se guarda con BCrypt (`password_hash`).
El RUN (sin puntos ni guion, ej. `20987654K`) es la clave primaria y se envía también como `id`.

| Método | Ruta | Detalle |
|---|---|---|
| GET | `/api/usuarios` | Filtros opcionales `?correo=` (minúsculas), `?run=` (acepta puntos/guion), `?tipo=`. Orden por nombre. |
| GET | `/api/usuarios/{id}` | `id` = RUN. 404 si no existe |
| POST | `/api/usuarios` | Obligatorios: `run` (módulo 11), `nombre` (≤50), `apellidos` (≤100), `correo` (@duoc.cl, @profesor.duoc.cl o @gmail.com), `password` (4-10). `tipo` por defecto `Cliente`. 409 si el RUN o el correo ya existen. |
| PUT | `/api/usuarios/{id}` | Reemplaza los datos. **Sin `password` se conserva la actual** (así `EditarUsuario` funciona aunque nunca reciba la clave). El `run` del cuerpo no puede ser distinto del de la ruta (400). |
| PATCH | `/api/usuarios/{id}` | Solo los campos enviados (perfil). Acepta `password` para cambiarla. Ignora cualquier intento de cambiar el `run`. |
| DELETE | `/api/usuarios/{id}` | 204. **409 si es el único administrador.** |
| POST | `/api/auth/login` | Cuerpo `{correo, password}` → 200 con el usuario **sin** contraseña. 401 con `{status, error, mensaje, campo}` donde `campo` es `"correo"` (no existe) o `"password"` (incorrecta). |
| GET | `/api/auth/salud` | `{ "servicio": "ms-usuarios", "estado": "OK" }` |

Campos: `id, run, nombre, apellidos, correo, fechaNacimiento (yyyy-MM-dd), tipo (Administrador|Vendedor|Cliente), region, comuna, direccion`.

Usuarios demo (se crean en el primer arranque si la tabla está vacía): `admin@duoc.cl / admin123`, `vendedor@duoc.cl / vende123`, `cliente@gmail.com / cliente123`.

Alcance de seguridad (limitación consciente): el login valida credenciales y devuelve el usuario, pero **la API no emite tokens (JWT)**. La protección de rutas (`RutaProtegida`) sigue siendo del front. Es suficiente para la pauta, pero un sistema real exigiría JWT en el gateway.

## ms-ordenes (implementado, etapa 17) · puerto 8083

Base `luxury_ordenes` (tablas `ordenes`, `orden_items`, `mensajes`). El `id` es numérico correlativo (autoincrement); el nº de boleta es `String(id).padStart(6, "0")` y lo arma el front. Las fechas se guardan en UTC y salen en ISO-8601 (`2026-10-05T22:40:00Z`).

### Órdenes `/api/ordenes`
| Método | Ruta | Detalle |
|---|---|---|
| GET | `/api/ordenes` | Filtros opcionales `?usuarioId=<run>` (historial, acepta puntos/guion) y `?estado=`. Orden `?_sort=fecha&_order=desc` (campos: `id, fecha, estado, subtotal, descuento, total`; otro campo → 400). Por defecto id ascendente. |
| GET | `/api/ordenes/{id}` | 404 si no existe |
| POST | `/api/ordenes` | 201 + `Location`. Mismo cuerpo que arma `Checkout.jsx` (ver `esquema-datos.md`). Compra como invitado = **sin** `usuarioId`. |
| PUT | `/api/ordenes/{id}` | Reemplaza la orden completa (conserva el id) |
| PATCH | `/api/ordenes/{id}` | **Solo `estado`** (`Pendiente`, `Pagado`, `Pago fallido`). Al pasar a `Pagado` el total se recalcula como `subtotal - descuento`. |
| DELETE | `/api/ordenes/{id}` | 204 |
| GET | `/api/ordenes/salud` | `{ "servicio": "ms-ordenes", "estado": "OK" }` |

Reglas de negocio del POST/PUT (400 si no se cumplen): al menos 1 producto (máx. 30); `cantidad` 1-99; `precio` 0-1.000.000; cada `items[].subtotal` = precio × cantidad y `subtotal` = suma de las líneas (el servidor las recalcula y rechaza si el cliente mandó otro valor); `descuento` ≤ subtotal; en una orden **no** fallida `total` = subtotal − descuento (un `Pago fallido` puede quedar con total 0, como lo envía el checkout); `pago.tarjetaFinal` = exactamente 4 dígitos (nunca el número completo); correo con formato válido (se guarda en minúsculas); `usuarioId` normalizado (sin puntos ni guion, K mayúscula).
`usuarioId` e `items[].productoId` son referencias lógicas: **no** se valida que existan en ms-usuarios / ms-catalogo (no hay llamadas entre servicios; la orden guarda una copia de los datos del producto).

### Mensajes de contacto `/api/mensajes`
`GET` (más recientes primero; `?_order=asc` invierte) · `GET /{id}` · `POST` (`nombre, correo, asunto, mensaje` de 10-2000 caracteres; `fecha` y `estado` son opcionales: por defecto ahora y `Nuevo`) · `PATCH /{id}` solo `{"estado": "Leído"}` (máx. 20 caracteres) · `DELETE /{id}`.

## Gateway (implementado, etapa 17) · puerto 8080
Spring Cloud Gateway (Boot 3.3.5 + Spring Cloud 2023.0.3). **Es la única URL que conoce el front**: `VITE_API_URL=http://localhost:8080/api`.

| Rutas | Se reenvían a |
|---|---|
| `/api/productos/**`, `/api/categorias/**`, `/api/ofertas/**`, `/api/catalogo/**` | ms-catalogo `:8081` |
| `/api/usuarios/**`, `/api/auth/**` | ms-usuarios `:8082` |
| `/api/ordenes/**`, `/api/mensajes/**` | ms-ordenes `:8083` |

Las rutas no se reescriben (cada microservicio ya usa `/api/...`). Las URLs destino se pueden cambiar con `MS_CATALOGO_URL`, `MS_USUARIOS_URL` y `MS_ORDENES_URL`.

### CORS
React (Vite, `http://localhost:5173`) y el gateway (`http://localhost:8080`) son orígenes distintos, así que el navegador exige CORS. Se configura **solo en el gateway** (`spring.cloud.gateway.globalcors` en `application.yml`); los microservicios no envían cabeceras CORS para que no lleguen duplicadas.
- Orígenes permitidos (lista blanca): `http://localhost:5173`, `http://127.0.0.1:5173` y `http://localhost:4173` (`vite preview`). Para otro origen: variable `CORS_ORIGINS` (separados por coma).
- Métodos: `GET, POST, PUT, PATCH, DELETE, OPTIONS`. Cabeceras: todas. `maxAge` 3600 s (el navegador reutiliza la pre-solicitud 1 hora).
- Sin cookies/credenciales (no hay sesión en el servidor, ver nota de seguridad).
- Un origen que no está en la lista recibe **403** en la pre-solicitud.

## Cambios hechos en el front (etapa 17)
1. `VITE_API_URL` apunta al gateway (`.env.example`; el valor por defecto de `api.js` ya era `:8080/api`).
2. `peticion()` (`api.js`) guarda en el error `status`, el cuerpo JSON del backend (`error.cuerpo`) y marca `status = 0` cuando no hay conexión (gateway apagado o CORS mal configurado).
3. `usuariosService.login` usa `POST /auth/login` y convierte el 401 (`campo` = `correo` | `password`) en el mismo `ErrorAuth` de antes: **`Login.jsx` no cambió**.
4. `_sort/_order` están soportados en ms-ordenes; `_expand=producto` en ms-catalogo (etapa 15).
5. json-server (`npm run api`, `mock-api/`) queda como respaldo histórico: usa el mismo puerto 8080 que el gateway (no ejecutar los dos a la vez) y ya no sirve para el login.
