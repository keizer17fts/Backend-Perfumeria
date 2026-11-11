# CRUD y Swagger/OpenAPI — Luxury Perfumería

## Alcance
Se mantuvieron los endpoints y formatos de las APIs para preservar compatibilidad con React. Se tomó como referencia el microservicio antiguo `ms_usuarios` en la organización de documentación con OpenAPI (`@Tag`, `@Operation`, `@ApiResponses` y `OpenAPIConfig`). **No se crearon tests ni datos de prueba**; tampoco se incorporó HATEOAS porque alteraría el JSON esperado por el frontend.

## Swagger UI (iniciar primero MySQL y cada microservicio)

| Servicio | Swagger UI | JSON OpenAPI |
| --- | --- | --- |
| Catálogo | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| Usuarios | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| Órdenes | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |

Cada página Swagger se sirve directamente desde su microservicio. El gateway de puerto 8080 **no** agrupa las tres interfaces Swagger. Swagger permite consultar los parámetros, cuerpos y respuestas a partir de los tipos Java, con operaciones agrupadas y respuestas HTTP anotadas.

## CRUD documentado

- Catálogo: `/api/productos` (GET/POST/PUT/PATCH/DELETE), `/api/categorias` (GET/POST/PUT/DELETE), `/api/ofertas` (GET/POST/PUT/DELETE), `/api/cupones` (GET/POST/PUT/DELETE y `/validar`), `/api/resenas` (GET/POST/DELETE), alertas de stock (POST).
- Usuarios: `/api/usuarios` (GET/POST/PUT/PATCH/DELETE, cambio de contraseña), `/api/auth/login`.
- Órdenes: `/api/ordenes` (GET/POST/PUT/PATCH/DELETE y PDF de boleta), `/api/mensajes` (GET/POST/PATCH/DELETE).

Los recursos no necesariamente ofrecen todas las operaciones de CRUD: por ejemplo, las reseñas se publican y eliminan, y las alertas se solicitan. No se añadieron métodos ficticios que no existían en la lógica de negocio.

## Mejoras puntuales

- Documentación integrada en controllers, agrupación de endpoints, respuestas HTTP y metadatos OpenAPI por servicio.
- Cupones: normalización de código con comprobación de nulos, listado ordenado, `201 Created` con `Location`, y transacciones en operaciones de escritura.
- Se mantienen validaciones y manejadores globales de errores existentes, servicios, repositorios y rutas para evitar cambios incompatibles con el frontend.

## Notas de ejecución

Requiere Java 21, Maven y MySQL configurados según el README y propiedades existentes. Al modificar dependencias Maven, recargue los proyectos en IntelliJ/Eclipse. No se ejecutaron compilaciones ni servicios en este entorno, por lo que conviene verificar el arranque en el equipo del proyecto.
