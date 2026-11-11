package cl.luxury.catalogo.service;

import cl.luxury.catalogo.exception.ConflictoException;
import cl.luxury.catalogo.exception.NoEncontradoException;
import cl.luxury.catalogo.exception.SolicitudInvalidaException;
import cl.luxury.catalogo.model.Producto;
import cl.luxury.catalogo.repository.CategoriaRepository;
import cl.luxury.catalogo.repository.OfertaRepository;
import cl.luxury.catalogo.repository.ProductoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProductoService {

    /** Filtros permitidos en GET /api/productos?campo=valor (igual que json-server). */
    private static final Set<String> CAMPOS_FILTRABLES =
            Set.of("categoria", "genero", "familia", "estacion", "tipo", "marca", "concentracion", "codigo");

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final OfertaRepository ofertas;
    private final ObjectMapper mapper;
    private final Validator validador;
    private final AlertaStockService alertas;

    public ProductoService(ProductoRepository productos, CategoriaRepository categorias,
                           OfertaRepository ofertas, ObjectMapper mapper, Validator validador, AlertaStockService alertas) {
        this.productos = productos;
        this.categorias = categorias;
        this.ofertas = ofertas;
        this.mapper = mapper;
        this.validador = validador;
        this.alertas = alertas;
    }

    @Transactional(readOnly = true)
    public List<Producto> listar(Map<String, String> filtros) {
        Specification<Producto> spec = (root, query, cb) -> cb.conjunction();

        for (Map.Entry<String, String> filtro : filtros.entrySet()) {
            final String campo = filtro.getKey();
            final String valor = filtro.getValue().toLowerCase();
            if (CAMPOS_FILTRABLES.contains(campo)) {
                spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get(campo)), valor));
            } else if ("q".equals(campo) && !valor.isBlank()) {
                final String patron = "%" + valor + "%";
                spec = spec.and((root, query, cb) -> cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("marca")), patron)));
            }
        }
        return productos.findAll(spec, Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Producto obtener(Long id) {
        return productos.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el producto " + id));
    }

    @Transactional
    public Producto crear(Producto producto) {
        producto.setId(null);
        validarReglas(producto);
        return productos.save(producto);
    }

    /** PUT: reemplaza el producto completo. */
    @Transactional
    public Producto actualizar(Long id, Producto producto) {
        Producto anterior = obtener(id);
        boolean agotado = anterior.getStock() == null || anterior.getStock() <= 0;
        producto.setId(id);
        validarReglas(producto);
        Producto guardado = productos.save(producto);
        if (agotado && guardado.getStock() != null && guardado.getStock() > 0) alertas.avisarReposicion(id);
        return guardado;
    }

    /** PATCH: cambia solo los campos enviados (ej. { "stock": 5 }). */
    @Transactional
    public Producto modificar(Long id, JsonNode cambios) {
        Producto actual = obtener(id);
        boolean agotado = actual.getStock() == null || actual.getStock() <= 0;
        try {
            mapper.readerForUpdating(actual).readValue(cambios);
        } catch (IOException e) {
            throw new SolicitudInvalidaException("Los cambios enviados no son válidos.");
        }
        actual.setId(id);

        Set<ConstraintViolation<Producto>> errores = validador.validate(actual);
        if (!errores.isEmpty()) {
            throw new ConstraintViolationException(errores);
        }
        validarReglas(actual);
        Producto guardado = productos.save(actual);
        if (agotado && guardado.getStock() != null && guardado.getStock() > 0) alertas.avisarReposicion(id);
        return guardado;
    }

    /** Borra el producto y sus ofertas. Las órdenes ya emitidas conservan su copia en items[]. */
    @Transactional
    public void eliminar(Long id) {
        obtener(id);
        ofertas.deleteByProductoId(id);
        productos.deleteById(id);
    }

    private void validarReglas(Producto p) {
        boolean codigoRepetido = p.getId() == null
                ? productos.existsByCodigo(p.getCodigo())
                : productos.existsByCodigoAndIdNot(p.getCodigo(), p.getId());
        if (codigoRepetido) {
            throw new ConflictoException("Ya existe un producto con el código " + p.getCodigo());
        }
        if (p.getCategoria() != null && !p.getCategoria().isBlank() && !categorias.existsById(p.getCategoria())) {
            throw new SolicitudInvalidaException("La categoría '" + p.getCategoria() + "' no existe.");
        }
    }
}
