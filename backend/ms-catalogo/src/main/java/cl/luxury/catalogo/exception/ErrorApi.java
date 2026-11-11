package cl.luxury.catalogo.exception;

/** Cuerpo estándar de error: { "status": 404, "error": "Not Found", "mensaje": "..." } */
public record ErrorApi(int status, String error, String mensaje) {
}
