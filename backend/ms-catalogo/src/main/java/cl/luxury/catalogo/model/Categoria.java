package cl.luxury.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Categoría = intensidad del perfume. El id es un slug: ligeros, moderados, intensos. */
@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
public class Categoria {

    @Id
    @Size(max = 50)
    private String id;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @Size(max = 500)
    @Column(length = 500)
    private String descripcion = "";

    @Size(max = 500)
    @Column(length = 500)
    private String imagen;
}
