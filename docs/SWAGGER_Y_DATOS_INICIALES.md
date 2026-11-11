# Swagger UI y datos iniciales — Luxury Perfumería

## Acceso local

Con los servicios encendidos, abrir las URL de Swagger UI:
- Catálogo: http://localhost:8081/swagger-ui/index.html
- Usuarios: http://localhost:8082/swagger-ui/index.html
- Órdenes: http://localhost:8083/swagger-ui/index.html

Abrir un grupo, seleccionar operación y pulsar **Try it out**, completar parámetros y JSON, después **Execute**. Las respuestas HTTP y cuerpos aparecen debajo. Las operaciones `POST`, `PATCH`, `PUT` y `DELETE` modifican datos reales del entorno configurado: probarlas sobre una base de pruebas.

## Referencias de uso

- `GET /api/v1/productos?categoria=intensos&genero=hombre&q=sauvage`: filtra productos.
- `GET /api/v1/ordenes?usuarioId=20987654K&_sort=fecha&_order=desc`: historial por usuario.
- `PATCH /api/v1/ordenes/{id}` con `{"estado":"En preparación"}`: transición de estado permitida solo desde un estado válido.
- `PUT /api/v1/ordenes/{id}`: reemplazo completo solo para una compra editable; no se puede reemplazar una compra ya pagada.
- `DELETE /api/v1/ordenes/{id}`: 204 si se elimina, 400 si una regla impide eliminarla, 404 si no existe.
- `GET /api/v1/ordenes/{id}/boleta`: PDF si la compra está confirmada, 409 en otro caso.

## Usuarios de demostración (F)

En `ms-usuarios` la creación automática de las tres cuentas demo ahora está **desactivada por defecto**. Se puede activar de forma explícita con la variable de entorno `SEED_USUARIOS=true` antes de iniciar el servicio; solamente se insertan si la tabla de usuarios está vacía. `SEED_USUARIOS=false` las desactiva. Esto **no elimina** usuarios ya creados.

## Migraciones iniciales de catálogo y órdenes

`ms-catalogo/src/main/resources/db/migration/V2__datos_iniciales.sql` y `ms-ordenes/src/main/resources/db/migration/V2__datos_iniciales.sql` son migraciones Flyway versionadas con datos preexistentes del proyecto. **No se han borrado ni editado**: alterar una migración aplicada cambiaría su checksum y puede impedir iniciar bases ya existentes. Tampoco se agregaron datos ficticios nuevos.

No existe actualmente un interruptor `SEED_CATALOGO` / `SEED_ORDENES` que desactive solo esas V2 en una base de datos nueva. Para un arranque completamente vacío hay que separar esas inserciones del historial de Flyway y planificar una migración/base nueva sin perder el esquema; **no establecer `spring.flyway.enabled=false`**, porque eso omitiría también la creación/actualización de tablas.

En una base ya iniciada, desactivar las semillas no borra registros anteriores. No eliminar datos reales mediante scripts de limpieza salvo que se trate deliberadamente de una base de pruebas respaldada.
