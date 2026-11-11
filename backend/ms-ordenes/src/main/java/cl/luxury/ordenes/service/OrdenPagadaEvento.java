package cl.luxury.ordenes.service;

/** Se emite únicamente cuando una orden pasa por primera vez a Pagado. */
public record OrdenPagadaEvento(Long ordenId) { }
