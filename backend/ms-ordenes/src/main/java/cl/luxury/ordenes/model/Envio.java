package cl.luxury.ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dirección de despacho. En el JSON va anidado: "envio": {...}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Envio {

    @Column(name = "envio_calle", length = 200)
    private String calle;

    @Column(name = "envio_departamento", length = 50)
    private String departamento;

    @Column(name = "envio_region", length = 100)
    private String region;

    @Column(name = "envio_comuna", length = 100)
    private String comuna;

    @Column(name = "envio_indicaciones", length = 500)
    private String indicaciones;
}
