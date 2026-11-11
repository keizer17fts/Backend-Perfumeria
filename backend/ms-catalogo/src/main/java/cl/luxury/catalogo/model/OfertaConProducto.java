package cl.luxury.catalogo.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Respuesta de GET /api/ofertas?_expand=producto: la oferta con su producto dentro. */
public record OfertaConProducto(@JsonUnwrapped Oferta oferta, Producto producto) {
}
