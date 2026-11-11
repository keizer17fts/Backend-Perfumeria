package cl.luxury.ordenes.service;

import cl.luxury.ordenes.dto.OrdenRequest;
import cl.luxury.ordenes.exception.NoEncontradoException;
import cl.luxury.ordenes.exception.SolicitudInvalidaException;
import cl.luxury.ordenes.model.Cliente;
import cl.luxury.ordenes.model.Envio;
import cl.luxury.ordenes.model.Orden;
import cl.luxury.ordenes.model.OrdenItem;
import cl.luxury.ordenes.model.Pago;
import cl.luxury.ordenes.repository.OrdenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrdenService {

    private static final Logger LOG = LoggerFactory.getLogger(OrdenService.class);
    static final String PAGADO = "Pagado";
    static final String PAGO_FALLIDO = "Pago fallido";
    private static final String PENDIENTE = "Pendiente";
    private static final String PREPARACION = "En preparación";
    private static final String ENVIADO = "Enviado";
    private static final String ENTREGADO = "Entregado";
    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            PENDIENTE, Set.of(PAGADO, PAGO_FALLIDO),
            PAGO_FALLIDO, Set.of(PENDIENTE, PAGADO),
            PAGADO, Set.of(PREPARACION),
            PREPARACION, Set.of(ENVIADO),
            ENVIADO, Set.of(ENTREGADO),
            ENTREGADO, Set.of()
    );
    private static final Set<String> ESTADOS = Set.of("Pendiente", PAGADO, PAGO_FALLIDO, "En preparación", "Enviado", "Entregado");
    /** Campos por los que se puede ordenar (?_sort=fecha&_order=desc, como json-server). */
    private static final Set<String> ORDENABLES = Set.of("id", "fecha", "estado", "subtotal", "descuento", "total");
    private static final long MAXIMO_IMPORTE = Integer.MAX_VALUE;

    private final OrdenRepository ordenes;
    private final ApplicationEventPublisher eventos;
    private final InventarioClient inventario;

    public OrdenService(OrdenRepository ordenes, ApplicationEventPublisher eventos, InventarioClient inventario) {
        this.ordenes = ordenes;
        this.eventos = eventos;
        this.inventario = inventario;
    }

    /**
     * Filtros opcionales: ?usuarioId=<run> (historial de compras), ?estado=Pagado.
     * Orden: ?_sort=fecha&_order=desc (por defecto, id ascendente).
     */
    @Transactional(readOnly = true)
    public List<Orden> listar(Map<String, String> filtros) {
        Specification<Orden> spec = (root, query, cb) -> cb.conjunction();

        String usuarioId = filtros.get("usuarioId");
        if (usuarioId != null) {
            final String run = normalizarRun(usuarioId);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("usuarioId"), run));
        }
        String estado = filtros.get("estado");
        if (estado != null) {
            final String valor = estado.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("estado")), valor));
        }

        String campo = filtros.getOrDefault("_sort", "id");
        if (!ORDENABLES.contains(campo)) {
            throw new SolicitudInvalidaException("No se puede ordenar por '" + campo + "'. Campos válidos: " + ORDENABLES);
        }
        String sentido = filtros.getOrDefault("_order", "asc");
        if (!sentido.equalsIgnoreCase("asc") && !sentido.equalsIgnoreCase("desc")) {
            throw new SolicitudInvalidaException("_order debe ser asc o desc.");
        }
        Sort.Direction direccion = Sort.Direction.fromString(sentido);
        // el id desempata cuando dos órdenes tienen la misma fecha
        Sort orden = campo.equals("id") ? Sort.by(direccion, "id") : Sort.by(direccion, campo).and(Sort.by(direccion, "id"));
        return ordenes.findAll(spec, orden);
    }

    @Transactional(readOnly = true)
    public Orden obtener(Long id) {
        return ordenes.findById(id).orElseThrow(() -> new NoEncontradoException("No existe la orden " + id));
    }

    @Transactional
    public Orden crear(OrdenRequest solicitud) {
        Orden orden = new Orden();
        aplicar(orden, solicitud);
        Orden guardada = ordenes.saveAndFlush(orden);
        if (PAGADO.equals(guardada.getEstado())) {
            descontarYCompensar(guardada);
            eventos.publishEvent(new OrdenPagadaEvento(guardada.getId()));
        }
        return guardada;
    }

    /** Reemplaza la orden completa (conserva el id). */
    @Transactional
    public Orden actualizar(Long id, OrdenRequest solicitud) {
        Orden orden = obtener(id);
        String anterior = orden.getEstado();
        if (!Set.of(PENDIENTE, PAGO_FALLIDO).contains(anterior)) {
            throw new SolicitudInvalidaException("No se pueden reemplazar los datos de una compra pagada o despachada.");
        }
        validarTransicion(anterior, solicitud.estado());
        boolean pagadaAntes = PAGADO.equals(anterior);
        aplicar(orden, solicitud);
        Orden guardada = ordenes.save(orden);
        if (!pagadaAntes && PAGADO.equals(guardada.getEstado())) {
            descontarYCompensar(guardada);
            eventos.publishEvent(new OrdenPagadaEvento(guardada.getId()));
        }
        return guardada;
    }

    /**
     * PATCH: por ahora solo se puede cambiar el estado (flujo del checkout: Pendiente -> Pagado / Pago fallido).
     * Al pasar a Pagado el total se recalcula como subtotal - descuento (un pago fallido guarda total 0).
     */
    @Transactional
    public Orden modificar(Long id, JsonNode cambios) {
        Orden orden = obtener(id);
        if (cambios == null || !cambios.hasNonNull("estado")) {
            throw new SolicitudInvalidaException("El cuerpo debe incluir 'estado'. Solo se puede modificar el estado.");
        }
        String estado = cambios.get("estado").asText();
        if (!ESTADOS.contains(estado)) {
            throw new SolicitudInvalidaException("Estado inválido. Valores: " + ESTADOS);
        }
        String anterior = orden.getEstado();
        if (anterior.equals(estado)) return orden; // evitar notificaciones repetidas
        validarTransicion(anterior, estado);
        boolean pagadaAntes = PAGADO.equals(anterior);
        orden.setEstado(estado);
        if (PAGADO.equals(estado)) {
            orden.setTotal(orden.getSubtotal() - orden.getDescuento());
        }
        Orden guardada = ordenes.save(orden);
        if (!pagadaAntes && PAGADO.equals(guardada.getEstado())) {
            descontarYCompensar(guardada);
            eventos.publishEvent(new OrdenPagadaEvento(guardada.getId()));
        } else if (Set.of("En preparación", "Enviado", "Entregado").contains(estado)) {
            eventos.publishEvent(new OrdenEstadoActualizadoEvento(guardada.getId(), estado));
        }
        return guardada;
    }

    @Transactional
    public void eliminar(Long id) {
        Orden orden = obtener(id);
        if (PAGADO.equals(orden.getEstado()) || Set.of("En preparación", "Enviado", "Entregado").contains(orden.getEstado())) {
            throw new SolicitudInvalidaException("No se puede eliminar una compra pagada o despachada sin gestionar su devolución de stock.");
        }
        ordenes.delete(orden);
    }

    /** Política única para PUT y PATCH: no permite saltar etapas ni volver a una etapa anterior. */
    private static void validarTransicion(String anterior, String siguiente) {
        if (!ESTADOS.contains(siguiente)) {
            throw new SolicitudInvalidaException("Estado inválido. Valores: " + ESTADOS);
        }
        if (!anterior.equals(siguiente) && !TRANSICIONES.getOrDefault(anterior, Set.of()).contains(siguiente)) {
            throw new SolicitudInvalidaException("Transición no permitida: " + anterior + " → " + siguiente);
        }
    }

    /**
     * Cada servicio conserva su transacción local. Si la transacción de órdenes falla
     * después de descontar en catálogo, se intenta compensar la actualización remota.
     * No sustituye a una cola transaccional: un corte total del proceso requeriría
     * conciliación manual de las órdenes y de los registros de descuento.
     */
    private void descontarYCompensar(Orden orden) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("El descuento de inventario requiere una transacción activa.");
        }
        final long ordenId = orden.getId();
        // Se registra primero para cubrir también los errores tras la respuesta HTTP.
        final boolean[] descontado = {false};
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED && descontado[0]) {
                    try {
                        inventario.revertirDescuento(ordenId);
                    } catch (Exception e) {
                        LOG.error("Revisar manualmente stock de orden {}: falló la compensación", ordenId, e);
                    }
                }
            }
        });
        inventario.descontar(orden);
        descontado[0] = true;
    }

    // ------------------------------------------------------------------ reglas de negocio

    private void aplicar(Orden destino, OrdenRequest r) {
        // El precio enviado por el navegador nunca es autoridad: contrastar con catálogo.
        // Si cambió, solicitar al cliente revisar el nuevo total antes de pagar.
        Set<String> ids = new java.util.HashSet<>();
        for (OrdenRequest.ItemDto i : r.items()) {
            if (!ids.add(i.productoId().trim())) {
                throw new SolicitudInvalidaException("El producto " + i.productoId() + " está repetido en la orden.");
            }
            InventarioClient.ProductoActual actual = inventario.consultarProducto(i.productoId().trim());
            if (!actual.precio().equals(i.precio())) {
                throw new SolicitudInvalidaException("El precio del producto '" + actual.nombre()
                        + "' cambió a $" + actual.precio() + ". Actualiza el carrito y confirma el nuevo total.");
            }
            if (actual.stock() < i.cantidad()) {
                throw new SolicitudInvalidaException("No hay suficientes unidades de '" + actual.nombre() + "'.");
            }
        }

        // 1) Subtotales de cada línea y de la orden: se recalculan, y si el cliente envió otro valor, es un error.
        long suma = 0;
        for (OrdenRequest.ItemDto i : r.items()) {
            long subtotalItem = (long) i.precio() * i.cantidad();
            if (i.subtotal() != null && i.subtotal() != subtotalItem) {
                throw new SolicitudInvalidaException("El subtotal de '" + i.nombre() + "' no coincide con precio x cantidad.");
            }
            suma += subtotalItem;
        }
        if (suma > MAXIMO_IMPORTE) {
            throw new SolicitudInvalidaException("El importe de la orden es demasiado alto.");
        }
        if (r.subtotal() != null && r.subtotal() != suma) {
            throw new SolicitudInvalidaException("El subtotal de la orden no coincide con la suma de sus productos.");
        }

        // 2) Descuento y total. Un pago fallido puede quedar con total 0 (así lo envía el checkout).
        int subtotal = (int) suma;
        int descuento = r.descuento() == null ? 0 : r.descuento();
        if (descuento > subtotal) {
            throw new SolicitudInvalidaException("El descuento no puede superar el subtotal.");
        }
        String codigoCupon = vacioANulo(r.cupon());
        if (codigoCupon == null) {
            if (descuento != 0) {
                throw new SolicitudInvalidaException("No se puede aplicar descuento sin un cupón.");
            }
        } else {
            int porcentaje = inventario.porcentajeCupon(codigoCupon);
            int calculado = (int) Math.round((double) subtotal * porcentaje / 100);
            if (descuento != calculado) {
                throw new SolicitudInvalidaException("El descuento del cupón cambió. Actualiza el carrito antes de comprar.");
            }
        }
        int esperado = subtotal - descuento;
        int total = r.total() == null ? esperado : r.total();
        if (total > subtotal) {
            throw new SolicitudInvalidaException("El total no puede superar el subtotal.");
        }
        if (!PAGO_FALLIDO.equals(r.estado()) && total != esperado) {
            throw new SolicitudInvalidaException("El total debe ser subtotal - descuento (" + esperado + ").");
        }

        // 3) Copiar los datos a la entidad
        destino.setFecha(r.fecha() != null ? r.fecha() : Instant.now());
        destino.setEstado(r.estado());
        String run = vacioANulo(r.usuarioId()) == null ? null : normalizarRun(r.usuarioId());
        if (run != null && run.length() > 9) {
            throw new SolicitudInvalidaException("usuarioId no es un RUN válido.");
        }
        destino.setUsuarioId(run);

        Cliente cliente = new Cliente();
        cliente.setNombre(r.cliente().nombre().trim());
        cliente.setApellidos(r.cliente().apellidos().trim());
        cliente.setCorreo(r.cliente().correo().trim().toLowerCase());
        destino.setCliente(cliente);

        Envio envio = new Envio();
        envio.setCalle(r.envio().calle().trim());
        envio.setDepartamento(textoOVacio(r.envio().departamento()));
        envio.setRegion(r.envio().region().trim());
        envio.setComuna(r.envio().comuna().trim());
        envio.setIndicaciones(textoOVacio(r.envio().indicaciones()));
        destino.setEnvio(envio);

        // orphanRemoval borra las líneas anteriores al reemplazar (PUT)
        destino.getItems().clear();
        for (OrdenRequest.ItemDto i : r.items()) {
            OrdenItem item = new OrdenItem();
            item.setProductoId(i.productoId().trim());
            item.setCodigo(i.codigo());
            item.setNombre(i.nombre());
            item.setMarca(i.marca());
            item.setImagen(i.imagen());
            item.setPrecio(i.precio());
            item.setCantidad(i.cantidad());
            item.setSubtotal(i.precio() * i.cantidad());
            destino.getItems().add(item);
        }

        destino.setSubtotal(subtotal);
        destino.setCupon(codigoCupon == null ? null : codigoCupon.toUpperCase(java.util.Locale.ROOT));
        destino.setDescuento(descuento);
        destino.setTotal(total);

        Pago pago = new Pago();
        pago.setMetodo(r.pago().metodo().trim());
        pago.setTarjetaFinal(r.pago().tarjetaFinal());
        destino.setPago(pago);
    }

    /** RUN sin puntos, guion ni espacios y con la K en mayúscula (así se guarda en ms-usuarios). */
    static String normalizarRun(String run) {
        return run.replaceAll("[.\\-\\s]", "").toUpperCase();
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static String textoOVacio(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
