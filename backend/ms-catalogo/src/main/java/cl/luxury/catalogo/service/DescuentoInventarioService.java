package cl.luxury.catalogo.service;

import cl.luxury.catalogo.exception.ConflictoException;
import cl.luxury.catalogo.exception.SolicitudInvalidaException;
import cl.luxury.catalogo.model.Producto;
import cl.luxury.catalogo.model.DescuentoInventario;
import cl.luxury.catalogo.repository.ProductoRepository;
import cl.luxury.catalogo.repository.DescuentoInventarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class DescuentoInventarioService {
    public record Linea(@NotNull Long productoId, @NotNull @Min(1) Integer cantidad) {}
    public record Solicitud(@NotBlank String ordenId, @NotEmpty List<@Valid Linea> items) {}

    private final ProductoRepository productos;
    private final DescuentoInventarioRepository descuentos;
    public DescuentoInventarioService(ProductoRepository productos, DescuentoInventarioRepository descuentos) {
        this.productos = productos;
        this.descuentos = descuentos;
    }

    /** Reversión idempotente para compensar una orden que no se pudo confirmar. */
    @Transactional
    public void revertir(String ordenId) {
        DescuentoInventario registro = descuentos.findById(ordenId).orElse(null);
        if (registro == null) return;
        String detalle = registro.getDetalle();
        // Se interpreta exactamente el formato canónico escrito por descontar(): {id=cantidad, ...}
        Map<Long, Integer> cantidades = new TreeMap<>();
        String interior = detalle.substring(1, detalle.length() - 1).trim();
        if (!interior.isEmpty()) {
            for (String par : interior.split(", ")) {
                String[] partes = par.split("=", 2);
                if (partes.length != 2) throw new IllegalStateException("Registro de descuento inválido");
                cantidades.put(Long.parseLong(partes[0]), Integer.parseInt(partes[1]));
            }
        }
        for (var entrada : cantidades.entrySet()) {
            Producto producto = productos.bloquearPorId(entrada.getKey())
                    .orElseThrow(() -> new IllegalStateException("Producto de compensación no encontrado: " + entrada.getKey()));
            producto.setStock(Math.addExact(producto.getStock(), entrada.getValue()));
        }
        productos.flush();
        descuentos.delete(registro);
        descuentos.flush();
    }

    /** Transacción única: si una línea no tiene stock, no se descuenta ninguna. */
    @Transactional
    public void descontar(Solicitud solicitud) {
        if (solicitud.ordenId().length() > 100) throw new SolicitudInvalidaException("ordenId demasiado largo.");
        Map<Long, Integer> cantidades = new TreeMap<>();
        for (Linea item : solicitud.items()) {
            if (item == null || item.productoId() == null || item.cantidad() == null || item.cantidad() < 1)
                throw new SolicitudInvalidaException("Los productos y sus cantidades deben ser válidos.");
            cantidades.merge(item.productoId(), item.cantidad(), Math::addExact);
        }
        String detalle = cantidades.toString();
        Optional<DescuentoInventario> existente = descuentos.findById(solicitud.ordenId());
        if (existente.isPresent()) {
            if (!existente.get().getDetalle().equals(detalle))
                throw new ConflictoException("Esta orden ya descontó un conjunto diferente de productos.");
            return;
        }
        // Bloqueo de filas en orden estable para evitar sobreventa y reducir deadlocks.
        List<Producto> bloqueados = new ArrayList<>();
        for (var item : cantidades.entrySet()) {
            Producto producto = productos.bloquearPorId(item.getKey())
                    .orElseThrow(() -> new ConflictoException("No existe el producto " + item.getKey()));
            if (producto.getStock() == null || producto.getStock() < item.getValue())
                throw new ConflictoException("Stock insuficiente para " + producto.getNombre()
                        + ": disponibles " + producto.getStock() + ", solicitados " + item.getValue());
            bloqueados.add(producto);
        }
        int n = 0;
        for (var item : cantidades.entrySet()) {
            Producto producto = bloqueados.get(n++);
            producto.setStock(producto.getStock() - item.getValue());
        }
        descuentos.saveAndFlush(new DescuentoInventario(solicitud.ordenId(), detalle));
        productos.flush();
    }
}
