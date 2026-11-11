package cl.luxury.ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Forma de pago. Solo se guardan los 4 últimos dígitos: nunca el número completo de la tarjeta. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Column(name = "pago_metodo", length = 30)
    private String metodo;

    @Column(name = "pago_tarjeta_final", length = 4)
    private String tarjetaFinal;
}
