// Una oferta es un descuento (%) sobre un producto. El precio final NO se guarda:
// se calcula en el front como Math.round(precio * (1 - descuento / 100)) para no quedar desfasado.
export const ofertas = [
  { id: "1", productoId: "1", descuento: 15, etiqueta: "Verano", vigenteHasta: "2026-12-31", activa: true },
  { id: "2", productoId: "5", descuento: 20, etiqueta: "Liquidación", vigenteHasta: "2026-12-31", activa: true },
  { id: "3", productoId: "9", descuento: 10, etiqueta: "Semana Luxury", vigenteHasta: "2026-12-31", activa: true },
  { id: "4", productoId: "14", descuento: 25, etiqueta: "Últimas unidades", vigenteHasta: "2026-12-31", activa: true },
  { id: "5", productoId: "20", descuento: 12, etiqueta: "Semana Luxury", vigenteHasta: "2026-12-31", activa: true },
  { id: "6", productoId: "30", descuento: 30, etiqueta: "Liquidación", vigenteHasta: "2026-06-30", activa: false },
];
