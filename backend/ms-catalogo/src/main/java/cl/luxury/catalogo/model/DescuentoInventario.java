package cl.luxury.catalogo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "descuentos_inventario")
public class DescuentoInventario {
    @Id
    @Column(name = "orden_id", length = 100)
    private String ordenId;
    @Column(name = "detalle", nullable = false, length = 4000)
    private String detalle;
    protected DescuentoInventario() {}
    public DescuentoInventario(String ordenId, String detalle) {
        this.ordenId = ordenId;
        this.detalle = detalle;
    }
    public String getDetalle() { return detalle; }
}
