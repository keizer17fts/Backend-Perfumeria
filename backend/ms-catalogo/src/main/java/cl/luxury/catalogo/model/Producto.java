package cl.luxury.catalogo.model;

import cl.luxury.catalogo.config.JsonNodeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Producto del catálogo. El JSON usa los mismos nombres que el front (camelCase) y el id
 * viaja como texto ("1") para que React lo compare igual que con json-server.
 */
@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @NotBlank
    @Size(max = 30)
    private String codigo;

    @Min(0)
    private Integer ml = 0;

    @NotBlank
    @Size(max = 100)
    private String marca;

    @NotBlank
    @Size(max = 150)
    private String nombre;

    @NotNull
    @Min(0)
    private Integer precio;

    @NotNull
    @Min(0)
    private Integer stock = 0;

    @Min(0)
    @Column(name = "stock_critico")
    private Integer stockCritico = 2;

    /** Id (slug) de la categoría. */
    @Column(name = "categoria_id")
    private String categoria;

    private String familia;
    private String estacion;
    private String genero;
    private String tipo;
    private String concentracion;

    @Column(length = 500)
    private String imagen;

    @Column(length = 4000)
    private String descripcion;

    @Column(name = "hero_fondo", length = 500)
    private String heroFondo;

    @Column(name = "historia_titulo", length = 200)
    private String historiaTitulo;

    @Column(length = 4000)
    private String historia;

    @Column(name = "historia_imagen", length = 500)
    private String historiaImagen;

    @Column(name = "cta_fondo", length = 500)
    private String ctaFondo;

    @Column(name = "fondo_pagina", length = 500)
    private String fondoPagina;

    @Column(name = "cta_titulo", length = 200)
    private String ctaTitulo;

    @Column(name = "cta_texto", length = 500)
    private String ctaTexto;

    @Column(name = "nav_claro")
    private Boolean navClaro;

    @Column(name = "hero_ajuste", length = 100)
    private String heroAjuste;

    // ---- Datos anidados guardados como JSON ----

    @Convert(converter = JsonNodeConverter.class)
    @Column(length = 4000)
    private JsonNode humor;

    @Convert(converter = JsonNodeConverter.class)
    @Column(length = 4000)
    private JsonNode maridaje;

    @Convert(converter = JsonNodeConverter.class)
    @Column(length = 4000)
    private JsonNode notas;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "notas_imagenes", length = 4000)
    private JsonNode notasImagenes;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "inspirado_en", length = 4000)
    private JsonNode inspiradoEn;

    @Convert(converter = JsonNodeConverter.class)
    @Column(length = 4000)
    private JsonNode colores;
}
