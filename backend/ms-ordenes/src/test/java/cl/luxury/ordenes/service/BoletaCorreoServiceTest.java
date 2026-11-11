package cl.luxury.ordenes.service;

import cl.luxury.ordenes.model.*;
import cl.luxury.ordenes.repository.*;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BoletaCorreoServiceTest {
    @Test
    void adjuntaPdfYActualizaElEstadoSinDependerDelTipoDeLogin() throws Exception {
        OrdenRepository ordenes = mock(OrdenRepository.class);
        BoletaEnvioRepository envios = mock(BoletaEnvioRepository.class);
        BoletaPdfService pdf = mock(BoletaPdfService.class);
        JavaMailSender correo = mock(JavaMailSender.class);
        Map<Long, BoletaEnvio> tareas = new HashMap<>();
        when(envios.findById(anyLong())).thenAnswer(i -> Optional.ofNullable(tareas.get(i.getArgument(0))));
        when(envios.saveAndFlush(any())).thenAnswer(i -> { BoletaEnvio e = i.getArgument(0); tareas.put(e.getOrdenId(), e); return e; });
        when(envios.save(any())).thenAnswer(i -> i.getArgument(0));
        when(envios.reservar(anyLong(), any())).thenReturn(1);
        when(envios.findTop10ByEstadoAndProximoIntentoLessThanEqualOrderByProximoIntentoAsc(eq("PENDIENTE"), any()))
                .thenAnswer(i -> tareas.values().stream().filter(e -> e.getEstado().equals("PENDIENTE")).toList());
        when(pdf.generar(any())).thenReturn("%PDF-de-prueba".getBytes());
        List<MimeMessage> enviados = new ArrayList<>();
        when(correo.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        doAnswer(i -> { enviados.add(i.getArgument(0)); return null; }).when(correo).send(any(MimeMessage.class));
        BoletaCorreoService servicio = new BoletaCorreoService(ordenes, envios, pdf, correo, true,
                "tienda@example.com", "tienda@example.com", "clave-de-prueba", true);
        for (long id : new long[]{1L, 2L}) {
            Orden orden = new Orden(); orden.setId(id); orden.setFecha(Instant.now()); orden.setEstado("Pagado");
            orden.setUsuarioId(id == 1 ? "20987654K" : "G12345678");
            Cliente cliente = new Cliente(); cliente.setNombre("Cliente"); cliente.setCorreo("cliente" + id + "@example.com"); orden.setCliente(cliente);
            when(ordenes.findById(id)).thenReturn(Optional.of(orden));
            servicio.enviar(new OrdenPagadaEvento(id));
            assertEquals("PENDIENTE", servicio.consultar(id).get("estado"));
            servicio.procesarPendientes();
            assertEquals("ENVIADO", servicio.consultar(id).get("estado"));
            assertEquals("cliente" + id + "@example.com", enviados.get((int)id-1).getAllRecipients()[0].toString());
            assertTrue(enviados.get((int)id-1).getContent() instanceof jakarta.mail.Multipart);
        }
        verify(pdf, times(2)).generar(any());
        verify(correo, times(2)).send(any(MimeMessage.class));
    }
}
