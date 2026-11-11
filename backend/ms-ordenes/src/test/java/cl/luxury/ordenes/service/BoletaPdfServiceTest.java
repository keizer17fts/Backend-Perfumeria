package cl.luxury.ordenes.service;

import cl.luxury.ordenes.model.Cliente;
import cl.luxury.ordenes.model.Orden;
import cl.luxury.ordenes.model.OrdenItem;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class BoletaPdfServiceTest {
    @Test
    void generaPdfValidoParaOrdenPagada() throws Exception {
        Orden orden = new Orden();
        orden.setId(42L);
        orden.setFecha(Instant.parse("2026-10-15T12:00:00Z"));
        orden.setEstado("Pagado");
        Cliente cliente = new Cliente();
        cliente.setNombre("Ana"); cliente.setApellidos("Perez"); cliente.setCorreo("ana@example.com");
        orden.setCliente(cliente);
        OrdenItem item = new OrdenItem();
        item.setNombre("Perfume de prueba"); item.setCantidad(2); item.setSubtotal(30000);
        orden.getItems().add(item);
        orden.setSubtotal(30000); orden.setDescuento(0); orden.setTotal(30000);
        byte[] resultado = new BoletaPdfService().generar(orden);
        assertEquals("%PDF", new String(resultado, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        try (PDDocument documento = PDDocument.load(resultado)) {
            assertEquals(1, documento.getNumberOfPages());
        }
    }
}
