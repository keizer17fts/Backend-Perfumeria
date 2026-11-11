// Genera 02-seed-catalogo.sql a partir de los MISMOS seed/*.js que usa json-server.
// Uso (desde la raíz del proyecto):  node backend/database/generar-seed-catalogo.mjs
import { writeFileSync } from "node:fs";
import { categorias } from "../../seed/categorias.js";
import { productos } from "../../seed/productos.js";
import { ofertas } from "../../seed/ofertas.js";


// Validación previa: el código (SKU) es UNIQUE en MySQL, así que no puede repetirse.
const vistos = new Map();
for (const p of productos) {
  if (vistos.has(p.codigo)) throw new Error(`Código repetido ${p.codigo}: productos ${vistos.get(p.codigo)} y ${p.id}`);
  vistos.set(p.codigo, p.id);
}
const idsCategoria = new Set(categorias.map((c) => c.id));
for (const p of productos) if (!idsCategoria.has(p.categoria)) throw new Error(`Producto ${p.id} con categoría inexistente: ${p.categoria}`);

const q = (v) => (v === undefined || v === null ? "NULL" : "'" + String(v).replace(/\\/g, "\\\\").replace(/'/g, "''") + "'");
const n = (v) => (v === undefined || v === null ? "NULL" : Number(v));
const j = (v) => (v === undefined || v === null ? "NULL" : q(JSON.stringify(v)));
const b = (v) => (v === undefined || v === null ? "NULL" : v ? 1 : 0);

let sql = "-- GENERADO por generar-seed-catalogo.mjs (no editar a mano)\nUSE luxury_catalogo;\nSET NAMES utf8mb4;\n\n";

sql += "INSERT INTO categorias (id, nombre, descripcion, imagen) VALUES\n" +
  categorias.map((c) => `  (${q(c.id)}, ${q(c.nombre)}, ${q(c.descripcion)}, ${q(c.imagen)})`).join(",\n") + ";\n\n";

const cols = "id, codigo, ml, marca, nombre, precio, stock, stock_critico, categoria_id, familia, estacion, genero, tipo, concentracion, imagen, descripcion, hero_fondo, historia_titulo, historia, historia_imagen, cta_fondo, fondo_pagina, cta_titulo, cta_texto, nav_claro, hero_ajuste, humor, maridaje, notas, notas_imagenes, inspirado_en, colores";
sql += `INSERT INTO productos (${cols}) VALUES\n` + productos.map((p) =>
  "  (" + [n(p.id), q(p.codigo), n(p.ml), q(p.marca), q(p.nombre), n(p.precio), n(p.stock), n(p.stockCritico ?? 2),
    q(p.categoria), q(p.familia), q(p.estacion), q(p.genero), q(p.tipo), q(p.concentracion), q(p.imagen), q(p.descripcion),
    q(p.heroFondo), q(p.historiaTitulo), q(p.historia), q(p.historiaImagen), q(p.ctaFondo), q(p.fondoPagina), q(p.ctaTitulo), q(p.ctaTexto),
    b(p.navClaro), q(p.heroAjuste), j(p.humor), j(p.maridaje), j(p.notas), j(p.notasImagenes), j(p.inspiradoEn), j(p.colores)].join(", ") + ")"
).join(",\n") + ";\n\n";

sql += "INSERT INTO ofertas (id, producto_id, descuento, etiqueta, vigente_hasta, activa) VALUES\n" +
  ofertas.map((o) => `  (${n(o.id)}, ${n(o.productoId)}, ${n(o.descuento)}, ${q(o.etiqueta)}, ${q(o.vigenteHasta)}, ${b(o.activa)})`).join(",\n") + ";\n";

writeFileSync(new URL("./02-seed-catalogo.sql", import.meta.url), sql);
console.log(`OK: ${categorias.length} categorías, ${productos.length} productos, ${ofertas.length} ofertas`);
