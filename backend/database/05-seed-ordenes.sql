-- Generado por backend/database/generar-seed-ordenes.mjs desde seed/ordenes.js. No editar a mano.
-- Requiere haber ejecutado 04-schema-ordenes.sql. Los ids se fijan para que coincidan con el mock;
-- el AUTO_INCREMENT continúa desde el mayor id.
USE luxury_ordenes;
SET NAMES utf8mb4;
DELETE FROM orden_items;
DELETE FROM ordenes;

INSERT INTO ordenes (id, fecha, estado, usuario_id, cliente_nombre, cliente_apellidos, cliente_correo, envio_calle, envio_departamento, envio_region, envio_comuna, envio_indicaciones, subtotal, cupon, descuento, total, pago_metodo, pago_tarjeta_final) VALUES (1, '2026-09-12 15:30:00.000', 'Pagado', '20987654K', 'Camila', 'Soto', 'cliente@gmail.com', 'Av. Perú 456', NULL, 'Valparaíso', 'Viña del Mar', NULL, 167980, NULL, 0, 167980, 'Tarjeta', '4242');
INSERT INTO orden_items (orden_id, producto_id, codigo, nombre, marca, imagen, precio, cantidad, subtotal) VALUES (1, '1', 'LUX001', 'Turathi Blue for Men', 'Afnan', 'https://wonder.com.bo/wp-content/uploads/2025/05/turathi-afnan-notas.jpg', 28000, 1, 28000);
INSERT INTO orden_items (orden_id, producto_id, codigo, nombre, marca, imagen, precio, cantidad, subtotal) VALUES (1, '5', 'LUX005', 'Eros', 'Versace', 'https://fimgs.net/photogram/p1200/xu/dd/sjhF2oPsrTWGj1R7.jpg', 69990, 2, 139980);

INSERT INTO ordenes (id, fecha, estado, usuario_id, cliente_nombre, cliente_apellidos, cliente_correo, envio_calle, envio_departamento, envio_region, envio_comuna, envio_indicaciones, subtotal, cupon, descuento, total, pago_metodo, pago_tarjeta_final) VALUES (2, '2026-09-28 19:05:00.000', 'Pagado', '20987654K', 'Camila', 'Soto', 'cliente@gmail.com', 'Av. Perú 456', NULL, 'Valparaíso', 'Viña del Mar', NULL, 27990, 'LUXURY10', 2799, 25191, 'Tarjeta', '4242');
INSERT INTO orden_items (orden_id, producto_id, codigo, nombre, marca, imagen, precio, cantidad, subtotal) VALUES (2, '9', 'LUX009', 'Nitro Red', 'Dumont', 'https://fimgs.net/photogram/p1200/6g/8s/bZuLdAW7wKZY7PaH.jpg', 27990, 1, 27990);

INSERT INTO ordenes (id, fecha, estado, usuario_id, cliente_nombre, cliente_apellidos, cliente_correo, envio_calle, envio_departamento, envio_region, envio_comuna, envio_indicaciones, subtotal, cupon, descuento, total, pago_metodo, pago_tarjeta_final) VALUES (3, '2026-10-05 22:40:00.000', 'Pago fallido', NULL, 'Pedro', 'Hacker', 'pedro.hacker20@example.com', 'Los Crisantemos, Edificio Norte', '603', 'Región Metropolitana', 'Santiago', 'El martes no estaremos, dejarlo con el conserje.', 153990, NULL, 0, 153990, 'Tarjeta', '0001');
INSERT INTO orden_items (orden_id, producto_id, codigo, nombre, marca, imagen, precio, cantidad, subtotal) VALUES (3, '14', 'LUX014', 'Le Beau Paradise Garden', 'Jean Paul Gaultier', 'https://media.falabella.com/falabellaCL/50369047_6/w=1500,h=1500,fit=cover', 153990, 1, 153990);
