package cl.luxury.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo de POST/PUT/PATCH. Tiene "password" (en texto, solo de entrada); la entidad guarda únicamente el hash.
 * Los campos que el front envía y no están aquí (id, passwordConfirm...) se ignoran.
 */
@Getter
@Setter
@NoArgsConstructor
public class UsuarioRequest {

    /** Se acepta con puntos y guion; el servicio lo normaliza y valida el dígito verificador. */
    @NotBlank
    private String run;

    @NotBlank
    @Size(max = 50)
    private String nombre;

    @NotBlank
    @Size(max = 100)
    private String apellidos;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "(?i)^[^\\s@]+@(duoc\\.cl|profesor\\.duoc\\.cl|gmail\\.com)$",
            message = "Solo correos @duoc.cl, @profesor.duoc.cl o @gmail.com")
    private String correo;

    /** Obligatoria al crear; opcional al editar (si no viene se conserva la actual). */
    @Size(min = 4, max = 10, message = "Entre 4 y 10 caracteres")
    private String password;

    /** yyyy-MM-dd o vacío. */
    private String fechaNacimiento;

    @Pattern(regexp = "Administrador|Vendedor|Cliente", message = "Debe ser Administrador, Vendedor o Cliente")
    private String tipo;

    @Size(max = 100)
    private String region;

    @Size(max = 100)
    private String comuna;

    @Size(max = 300)
    private String direccion;

    /** Una contraseña en blanco se trata como "no enviada". */
    public void setPassword(String password) {
        this.password = (password == null || password.isBlank()) ? null : password;
    }
}
