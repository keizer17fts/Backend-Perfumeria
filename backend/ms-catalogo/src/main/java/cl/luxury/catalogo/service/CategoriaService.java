package cl.luxury.catalogo.service;

import cl.luxury.catalogo.exception.ConflictoException;
import cl.luxury.catalogo.exception.NoEncontradoException;
import cl.luxury.catalogo.exception.SolicitudInvalidaException;
import cl.luxury.catalogo.model.Categoria;
import cl.luxury.catalogo.repository.CategoriaRepository;
import cl.luxury.catalogo.repository.ProductoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;

    public CategoriaService(CategoriaRepository categorias, ProductoRepository productos) {
        this.categorias = categorias;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return categorias.findAll(Sort.by("nombre"));
    }

    @Transactional(readOnly = true)
    public Categoria obtener(String id) {
        return categorias.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la categoría " + id));
    }

    @Transactional
    public Categoria crear(Categoria categoria) {
        if (categoria.getId() == null || categoria.getId().isBlank()) {
            categoria.setId(slug(categoria.getNombre()));
        }
        if (categoria.getId().isBlank()) {
            throw new SolicitudInvalidaException("No se pudo generar un identificador para la categoría.");
        }
        if (categorias.existsById(categoria.getId())) {
            throw new ConflictoException("Ya existe una categoría con el identificador " + categoria.getId());
        }
        return categorias.save(categoria);
    }

    @Transactional
    public Categoria actualizar(String id, Categoria categoria) {
        obtener(id);
        categoria.setId(id);
        return categorias.save(categoria);
    }

    /** No se puede borrar una categoría que todavía tiene productos. */
    @Transactional
    public void eliminar(String id) {
        obtener(id);
        if (productos.countByCategoria(id) > 0) {
            throw new ConflictoException("La categoría tiene productos asociados y no se puede eliminar.");
        }
        categorias.deleteById(id);
    }

    static String slug(String texto) {
        String sinTildes = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}
