package cl.luxury.ordenes.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Línea de una orden. Copia nombre/precio/imagen del producto: si el precio cambia después, la boleta no cambia. */
@Entity
@Table(name = "orden_items")
@Getter
@Setter
@NoArgsConstructor
public class OrdenItem {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Referencia lógica a luxury_catalogo.productos (no hay clave foránea entre bases de datos). */
    @Column(name = "producto_id", length = 20)
    private String productoId;

    @Column(length = 50)
    private String codigo;

    @Column(length = 150)
    private String nombre;

    @Column(length = 100)
    private String marca;

    @Column(length = 300)
    private String imagen;

    private int precio;

    private int cantidad;

    private int subtotal;
}
