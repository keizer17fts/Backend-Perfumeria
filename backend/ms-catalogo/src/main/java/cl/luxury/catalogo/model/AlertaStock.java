package cl.luxury.catalogo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name = "alertas_stock") @Getter @Setter
public class AlertaStock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "producto_id", nullable = false) private Long productoId;
    @Column(name = "correo", nullable = false, length = 254) private String correo;
    @Column(name = "creada_en", nullable = false) private Instant creadaEn = Instant.now();
    @Column(nullable = false) private boolean enviada = false;
}
