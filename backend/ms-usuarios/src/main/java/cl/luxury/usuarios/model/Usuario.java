package cl.luxury.usuarios.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Usuario. El RUN (sin puntos ni guion) es la clave primaria y también se envía como "id"
 * para que el front siga funcionando igual que con json-server.
 * El hash de la contraseña NUNCA se serializa: no sale en ninguna respuesta de la API.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @Column(length = 9)
    private String run;

    @Column(length = 50)
    private String nombre;

    @Column(length = 100)
    private String apellidos;

    @Column(length = 100)
    private String correo;

    @JsonIgnore
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 20)
    private String tipo = "Cliente";

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String comuna;

    @Column(length = 300)
    private String direccion;

    @JsonIgnore
    @Column(name = "google_sub", length = 255, unique = true)
    private String googleSub;

    @JsonProperty("id")
    public String getId() {
        return run;
    }
}
