package cl.luxury.ordenes.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de compra. La misma orden es la boleta (el front muestra el documento a partir de estos datos).
 * El id numérico correlativo lo asigna la base de datos; el nº de boleta es el id con ceros a la izquierda.
 * Con Jackson en modo non_null, una compra como invitado simplemente no trae "usuarioId".
 */
@Entity
@Table(name = "ordenes")
@Getter
@Setter
@NoArgsConstructor
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Instante UTC; se serializa en ISO-8601 (2026-10-05T22:40:00Z), igual que el mock. */
    @Column(nullable = false)
    private Instant fecha;

    /** Pendiente | Pagado | Pago fallido */
    @Column(length = 20, nullable = false)
    private String estado;

    /** RUN del cliente (referencia lógica a ms-usuarios). Null = compra como invitado. */
    @Column(name = "usuario_id", length = 9)
    private String usuarioId;

    @Embedded
    private Cliente cliente;

    @Embedded
    private Envio envio;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "orden_id", nullable = false)
    @OrderBy("id ASC")
    private List<OrdenItem> items = new ArrayList<>();

    private int subtotal;

    @Column(length = 30)
    private String cupon;

    private int descuento;

    private int total;

    @Embedded
    private Pago pago;
}
