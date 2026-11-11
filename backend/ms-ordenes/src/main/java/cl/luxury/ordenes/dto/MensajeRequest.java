package cl.luxury.ordenes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Cuerpo de POST /api/mensajes (el formulario de Contacto exige mínimo 10 caracteres en el mensaje). */
public record MensajeRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 100) String correo,
        @NotBlank @Size(max = 150) String asunto,
        @NotBlank @Size(min = 10, max = 2000) String mensaje,
        Instant fecha,
        @Size(max = 20) String estado) {
}
