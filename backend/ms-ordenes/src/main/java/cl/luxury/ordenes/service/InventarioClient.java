package cl.luxury.ordenes.service;

import cl.luxury.ordenes.exception.SolicitudInvalidaException;
import cl.luxury.ordenes.model.Orden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.*;

@Component
public class InventarioClient {
    private final RestClient cliente;
    public InventarioClient(@Value("${luxury.catalogo.url:http://localhost:8081}") String url) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.cliente = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    public record ProductoActual(String id, String codigo, String nombre, String marca,
                                 String imagen, Integer precio, Integer stock) {}

    public ProductoActual consultarProducto(String id) {
        try {
            long numero = Long.parseLong(id);
            if (numero <= 0) throw new NumberFormatException();
            ProductoActual producto = cliente.get().uri("/api/productos/{id}", numero)
                    .retrieve().body(ProductoActual.class);
            if (producto == null || producto.precio() == null || producto.precio() < 0 || producto.stock() == null) {
                throw new IllegalStateException("Catálogo devolvió datos incompletos para el producto " + id);
            }
            return producto;
        } catch (NumberFormatException e) {
            throw new SolicitudInvalidaException("Identificador de producto inválido: " + id);
        } catch (HttpClientErrorException.NotFound e) {
            throw new SolicitudInvalidaException("El producto " + id + " ya no está disponible en catálogo.");
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo consultar el precio vigente del catálogo. No se confirmó la compra.", e);
        }
    }

    public int porcentajeCupon(String codigo) {
        try {
            JsonCupon cupon = cliente.get().uri("/api/cupones/{codigo}/validar", codigo)
                    .retrieve().body(JsonCupon.class);
            if (cupon == null || !Boolean.TRUE.equals(cupon.valido()) || cupon.descuento() == null
                    || cupon.descuento() < 1 || cupon.descuento() > 90) {
                throw new SolicitudInvalidaException("El cupón no tiene un descuento válido.");
            }
            return cupon.descuento();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().is4xxClientError())
                throw new SolicitudInvalidaException("El cupón ya no existe, está inactivo o expiró. Vuelve a aplicarlo.");
            throw e;
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo verificar el cupón en catálogo. La compra no se confirmó.", e);
        }
    }

    private record JsonCupon(String codigo, Integer descuento, Boolean valido) {}

    public void revertirDescuento(long ordenId) {
        try {
            cliente.post().uri("/api/inventario/revertir/{ordenId}", "ordenes:" + ordenId)
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo revertir automáticamente el inventario de la orden " + ordenId, e);
        }
    }

    public void descontar(Orden orden) {
        var lineas = orden.getItems().stream().map(i -> {
            try {
                return Map.of("productoId", Long.parseLong(i.getProductoId()), "cantidad", i.getCantidad());
            } catch (NumberFormatException e) {
                throw new SolicitudInvalidaException("Identificador de producto inválido: " + i.getProductoId());
            }
        }).toList();
        try {
            cliente.post().uri("/api/inventario/descontar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("ordenId", "ordenes:" + orden.getId(), "items", lineas))
                    .retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new SolicitudInvalidaException("No se pudo confirmar la compra: stock insuficiente o inventario incompatible.");
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo verificar el inventario con catálogo. La compra no se confirmó.", e);
        }
    }
}
