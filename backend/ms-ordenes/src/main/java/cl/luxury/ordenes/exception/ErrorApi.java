package cl.luxury.ordenes.exception;

/** { "status": 400, "error": "Bad Request", "mensaje": "..." } (mismo formato que los otros servicios) */
public record ErrorApi(int status, String error, String mensaje) {
}
