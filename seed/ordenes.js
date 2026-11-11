// Ordenes de ejemplo para que Admin (ordenes, boletas, reportes) y el historial de compras
// tengan datos desde el inicio. Los items guardan una "foto" del producto al momento de la compra.
import { productos } from "./productos.js";
import { CUPONES } from "../src/utils/cupones.js";

// Fuente única de cupones (porcentajes). Mismo archivo que usa el carrito en el front.

function item(productoId, cantidad) {
  const p = productos.find((x) => x.id === productoId);
  return {
    productoId: p.id,
    codigo: p.codigo,
    nombre: p.nombre,
    marca: p.marca,
    imagen: p.imagen,
    precio: p.precio,
    cantidad,
    subtotal: p.precio * cantidad,
  };
}

function orden({ id, fecha, usuarioId, cliente, envio, items, cupon = null, estado, tarjetaFinal }) {
  const subtotal = items.reduce((s, i) => s + i.subtotal, 0);
  const descuento = cupon ? Math.round((subtotal * CUPONES[cupon]) / 100) : 0;
  const orden = {
    id, fecha, estado, cliente, envio, items,
    subtotal, cupon, descuento, total: subtotal - descuento,
    pago: { metodo: "Tarjeta", tarjetaFinal },
  };
  // Compra como invitado = SIN el campo usuarioId. Nunca usuarioId: null (ver docs/esquema-datos.md).
  if (usuarioId) orden.usuarioId = usuarioId;
  return orden;
}

const camila = {
  usuarioId: "20987654K",
  cliente: { nombre: "Camila", apellidos: "Soto", correo: "cliente@gmail.com" },
  envio: { calle: "Av. Perú 456", departamento: "", region: "Valparaíso", comuna: "Viña del Mar", indicaciones: "" },
};

export const ordenes = [
  orden({ id: 1, fecha: "2026-09-12T15:30:00.000Z", ...camila, items: [item("1", 1), item("5", 2)], estado: "Pagado", tarjetaFinal: "4242" }),
  orden({ id: 2, fecha: "2026-09-28T19:05:00.000Z", ...camila, items: [item("9", 1)], cupon: "LUXURY10", estado: "Pagado", tarjetaFinal: "4242" }),
  orden({
    id: 3, fecha: "2026-10-05T22:40:00.000Z", usuarioId: null,
    cliente: { nombre: "Pedro", apellidos: "Hacker", correo: "pedro.hacker20@example.com" },
    envio: { calle: "Los Crisantemos, Edificio Norte", departamento: "603", region: "Región Metropolitana", comuna: "Santiago", indicaciones: "El martes no estaremos, dejarlo con el conserje." },
    items: [item("14", 1)], estado: "Pago fallido", tarjetaFinal: "0001",
  }),
];
