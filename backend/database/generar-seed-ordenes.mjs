// Genera backend/database/05-seed-ordenes.sql desde seed/ordenes.js (las mismas 3 órdenes del mock).
// Uso (desde la raíz del proyecto):  npm run db:seed-ordenes
import { writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { ordenes } from "../../seed/ordenes.js";

const q = (v) => (v === null || v === undefined || v === "" ? "NULL" : `'${String(v).replace(/\\/g, "\\\\").replace(/'/g, "''")}'`);
const n = (v) => Number(v);
const fechaSql = (iso) => new Date(iso).toISOString().replace("T", " ").replace("Z", ""); // UTC, con milisegundos

const lineas = [
  "-- Generado por backend/database/generar-seed-ordenes.mjs desde seed/ordenes.js. No editar a mano.",
  "-- Requiere haber ejecutado 04-schema-ordenes.sql. Los ids se fijan para que coincidan con el mock;",
  "-- el AUTO_INCREMENT continúa desde el mayor id.",
  "USE luxury_ordenes;",
  "SET NAMES utf8mb4;",
  "DELETE FROM orden_items;",
  "DELETE FROM ordenes;",
  "",
];

for (const o of ordenes) {
  lineas.push(
    "INSERT INTO ordenes (id, fecha, estado, usuario_id, cliente_nombre, cliente_apellidos, cliente_correo, " +
      "envio_calle, envio_departamento, envio_region, envio_comuna, envio_indicaciones, " +
      "subtotal, cupon, descuento, total, pago_metodo, pago_tarjeta_final) VALUES (" +
      [
        n(o.id), q(fechaSql(o.fecha)), q(o.estado), q(o.usuarioId),
        q(o.cliente.nombre), q(o.cliente.apellidos), q(o.cliente.correo),
        q(o.envio.calle), q(o.envio.departamento), q(o.envio.region), q(o.envio.comuna), q(o.envio.indicaciones),
        n(o.subtotal), q(o.cupon), n(o.descuento), n(o.total), q(o.pago.metodo), q(o.pago.tarjetaFinal),
      ].join(", ") + ");"
  );
  for (const i of o.items) {
    lineas.push(
      "INSERT INTO orden_items (orden_id, producto_id, codigo, nombre, marca, imagen, precio, cantidad, subtotal) VALUES (" +
        [n(o.id), q(i.productoId), q(i.codigo), q(i.nombre), q(i.marca), q(i.imagen), n(i.precio), n(i.cantidad), n(i.subtotal)].join(", ") + ");"
    );
  }
  lineas.push("");
}

const destino = join(dirname(fileURLToPath(import.meta.url)), "05-seed-ordenes.sql");
writeFileSync(destino, lineas.join("\n"), "utf8");
console.log(`OK: ${ordenes.length} órdenes -> ${destino}`);
