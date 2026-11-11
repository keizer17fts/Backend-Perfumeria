// Cupones de descuento. Fuente única: también la usa seed/ordenes.js.
// El valor es un porcentaje entero (10 = 10 %), igual que `ofertas.descuento` y que las órdenes guardadas.
// Importante: este archivo no debe depender de React ni de import.meta.env (lo ejecuta Node al generar db.json).
// Con el backend (etapa 15-17) esta tabla pasa a ser un endpoint /cupones y estas funciones quedan como respaldo.

export const CUPONES = {
  LUXURY10: 10,
  BIENVENIDO15: 15,
  NAVIDAD20: 20,
};

export function normalizarCodigo(codigo) {
  return String(codigo ?? "").trim().toUpperCase();
}

export function cuponValido(codigo) {
  return Object.prototype.hasOwnProperty.call(CUPONES, normalizarCodigo(codigo));
}

// Porcentaje del cupón, o 0 si el código no existe.
export function porcentajeCupon(codigo) {
  const c = normalizarCodigo(codigo);
  return cuponValido(c) ? CUPONES[c] : 0;
}

export function calcularDescuento(subtotal, codigo) {
  return Math.round((subtotal * porcentajeCupon(codigo)) / 100);
}

export function calcularTotales(subtotal, codigo) {
  const descuento = calcularDescuento(subtotal, codigo);
  return { subtotal, descuento, total: subtotal - descuento };
}
