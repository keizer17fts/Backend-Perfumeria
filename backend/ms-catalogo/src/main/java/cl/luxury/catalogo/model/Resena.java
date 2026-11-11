package cl.luxury.catalogo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name = "resenas") @Getter @Setter
public class Resena {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "producto_id", nullable = false) private Long productoId;
    @Column(name = "usuario_id", nullable = false, length = 30) private String usuarioId;
    @Column(nullable = false, length = 150) private String nombre;
    @Column(nullable = false) private int estrellas;
    @Column(nullable = false, length = 1000) private String texto;
    @Column(nullable = false) private Instant fecha = Instant.now();
}
