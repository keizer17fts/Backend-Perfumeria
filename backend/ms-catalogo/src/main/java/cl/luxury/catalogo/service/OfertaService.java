package cl.luxury.catalogo.service;

import cl.luxury.catalogo.exception.NoEncontradoException;
import cl.luxury.catalogo.exception.SolicitudInvalidaException;
import cl.luxury.catalogo.model.Oferta;
import cl.luxury.catalogo.model.OfertaConProducto;
import cl.luxury.catalogo.model.Producto;
import cl.luxury.catalogo.repository.OfertaRepository;
import cl.luxury.catalogo.repository.ProductoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OfertaService {

    private final OfertaRepository ofertas;
    private final ProductoRepository productos;

    public OfertaService(OfertaRepository ofertas, ProductoRepository productos) {
        this.ofertas = ofertas;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<Oferta> listar(Boolean activa) {
        return activa == null ? ofertas.findAll(Sort.by("id")) : ofertas.findByActivaOrderByIdAsc(activa);
    }

    /** Equivalente a ?_expand=producto de json-server. */
    @Transactional(readOnly = true)
    public List<OfertaConProducto> listarConProducto(Boolean activa) {
        List<Oferta> lista = listar(activa);
        List<Long> ids = lista.stream().map(Oferta::getProductoId).distinct().toList();
        Map<Long, Producto> porId = productos.findAllById(ids).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));
        return lista.stream()
                .map(o -> new OfertaConProducto(o, porId.get(o.getProductoId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Oferta obtener(Long id) {
        return ofertas.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la oferta " + id));
    }

    @Transactional
    public Oferta crear(Oferta oferta) {
        oferta.setId(null);
        exigirProducto(oferta);
        return ofertas.save(oferta);
    }

    @Transactional
    public Oferta actualizar(Long id, Oferta oferta) {
        obtener(id);
        oferta.setId(id);
        exigirProducto(oferta);
        return ofertas.save(oferta);
    }

    @Transactional
    public void eliminar(Long id) {
        obtener(id);
        ofertas.deleteById(id);
    }

    private void exigirProducto(Oferta oferta) {
        if (!productos.existsById(oferta.getProductoId())) {
            throw new SolicitudInvalidaException("El producto " + oferta.getProductoId() + " no existe.");
        }
    }
}
