package cl.luxury.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioPasswordRequest(
        @NotBlank String correo,
        @NotBlank String passwordActual,
        @NotBlank @Size(min = 4, max = 10) String passwordNueva) { }
