package cl.luxury.catalogo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity @Table(name = "cupones") @Getter @Setter
public class Cupon {
    @Id @Column(length = 40) @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,40}")
    private String codigo;
    @NotNull @Min(1) @Max(90) private Integer descuento;
    @NotNull private Boolean activo = true;
    @Column(name = "vigente_desde") private LocalDate vigenteDesde;
    @Column(name = "vigente_hasta") private LocalDate vigenteHasta;
}
