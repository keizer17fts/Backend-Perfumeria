# Mejoras de integración — 2 de noviembre de 2026

Esta entrega mantiene React, las rutas REST existentes, las bases de datos y sus migraciones. No agrega tests ni datos ficticios.

## Cupón verificado por órdenes

`OrdenService.aplicar()` solicita a catálogo `GET /api/v1/cupones/{codigo}/validar`, comprueba la vigencia y el porcentaje del cupón y recalcula el descuento con redondeo CLP (`Math.round`), igual que el carrito. Sin cupón, el descuento debe ser cero. Si el porcentaje cambió, la operación se rechaza para que el usuario actualice el carrito. Un error de conectividad impide confirmar la compra.

## Descuento y compensación de inventario

Al pasar una orden a `Pagado`, órdenes llama `POST /api/v1/inventario/descontar` con un identificador idempotente `ordenes:{id}`. Si la transacción de base de datos de órdenes **no se confirma** tras el descuento, un callback de finalización intenta devolver las existencias mediante `POST /api/v1/inventario/revertir/{ordenId}`. Catálogo bloquea los productos durante la operación, suma las cantidades originales y borra el registro de descuento dentro de una transacción. Una segunda reversión responde 204, sin sumar dos veces.

**Limitación:** no hay transacción distribuida. Una caída total de procesos, timeout de red ambiguo o fallo de la compensación todavía puede dejar datos inconsistentes. En esos casos hay que revisar manualmente las órdenes de pago y la tabla `descuentos_inventario`. No se incorporaron colas de mensajería, reintentos automáticos persistentes ni un proceso de conciliación.

## Swagger

Catálogo: `http://localhost:8081/swagger-ui.html`.

- `POST /api/v1/inventario/descontar`: JSON `{ "ordenId": "ordenes:123", "items": [{ "productoId": 1, "cantidad": 2 }] }`. Respuestas: 204, 400, 409.
- `POST /api/v1/inventario/revertir/{ordenId}`: por ejemplo, `ordenes:123`. Respuestas: 204 o 500.
- `GET /api/v1/cupones/{codigo}/validar`: retorna `{ "codigo": "...", "descuento": 10, "valido": true }` para un cupón vigente; los inactivos o vencidos generan 400 y los inexistentes, 404.

Órdenes: `http://localhost:8083/swagger-ui.html`.

- `POST /api/v1/ordenes`: valida importes, precio de catálogo, existencia de stock y porcentaje de cupón.
- `PATCH /api/v1/ordenes/{id}` con `{ "estado": "Pagado" }`: descuenta el inventario y emite el evento de compra.

## Comprobación manual sugerida

Con los servicios ejecutándose, utiliza el flujo de compra existente y verifica los cambios en el panel de catálogo y órdenes. Comprueba que el cupón inactivo sea rechazado y que el descuento corresponda al porcentaje vigente. No se añadieron pruebas automatizadas.

## Nota de ejecución

No se pudo compilar con Maven en el entorno de preparación porque no hay ejecutable `mvn` ni wrapper incluido. Revisar la compilación local en el IDE antes de sustituir una versión que ya funciona.
