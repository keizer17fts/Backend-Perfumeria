package cl.luxury.ordenes.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name = "boleta_envios") @Getter @Setter
public class BoletaEnvio {
    @Id private Long ordenId;
    @Column(nullable = false, length = 20) private String estado = "PENDIENTE";
    private int intentos;
    private Instant actualizado = Instant.now();
    private Instant proximoIntento = Instant.now();
    @Column(length = 300) private String mensaje;
}
