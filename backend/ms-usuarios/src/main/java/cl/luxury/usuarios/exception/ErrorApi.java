package cl.luxury.usuarios.exception;

/** { "status": 401, "error": "Unauthorized", "mensaje": "...", "campo": "password" }  (campo solo en el login) */
public record ErrorApi(int status, String error, String mensaje, String campo) {
}
