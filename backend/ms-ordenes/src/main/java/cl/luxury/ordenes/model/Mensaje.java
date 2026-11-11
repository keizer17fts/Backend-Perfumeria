package cl.luxury.ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Mensaje enviado desde el formulario de Contacto. */
@Entity
@Table(name = "mensajes")
@Getter
@Setter
@NoArgsConstructor
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String nombre;

    @Column(length = 100, nullable = false)
    private String correo;

    @Column(length = 150, nullable = false)
    private String asunto;

    @Column(length = 2000, nullable = false)
    private String mensaje;

    @Column(nullable = false)
    private Instant fecha;

    /** Nuevo | Leído | Respondido (texto libre de hasta 20 caracteres) */
    @Column(length = 20, nullable = false)
    private String estado = "Nuevo";
}
