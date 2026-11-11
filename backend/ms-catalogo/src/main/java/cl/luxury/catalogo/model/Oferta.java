package cl.luxury.catalogo.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Descuento (%) sobre un producto. El precio final no se guarda: lo calcula el front. */
@Entity
@Table(name = "ofertas")
@Getter
@Setter
@NoArgsConstructor
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @NotNull
    @Column(name = "producto_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productoId;

    @NotNull
    @Min(1)
    @Max(90)
    private Integer descuento;

    private String etiqueta;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @NotNull
    private Boolean activa = Boolean.TRUE;
}
