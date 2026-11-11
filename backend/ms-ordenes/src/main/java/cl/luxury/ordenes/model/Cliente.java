package cl.luxury.ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos del comprador (copia al momento de la compra). En el JSON va anidado: "cliente": {...}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Column(name = "cliente_nombre", length = 50)
    private String nombre;

    @Column(name = "cliente_apellidos", length = 100)
    private String apellidos;

    @Column(name = "cliente_correo", length = 100)
    private String correo;
}
