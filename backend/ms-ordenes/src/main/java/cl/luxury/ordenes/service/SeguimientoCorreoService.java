package cl.luxury.ordenes.service;

import cl.luxury.ordenes.model.Orden;
import cl.luxury.ordenes.repository.OrdenRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class SeguimientoCorreoService {
    private static final Logger log = LoggerFactory.getLogger(SeguimientoCorreoService.class);
    private final OrdenRepository ordenes;
    private final JavaMailSender correo;
    private final boolean habilitado;
    private final String remitente;

    public SeguimientoCorreoService(OrdenRepository ordenes, JavaMailSender correo,
            @Value("${luxury.mail.enabled:false}") boolean habilitado,
            @Value("${luxury.mail.from:}") String remitente) {
        this.ordenes = ordenes;
        this.correo = correo;
        this.habilitado = habilitado;
        this.remitente = remitente;
    }

    @Async("correoExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(OrdenEstadoActualizadoEvento evento) {
        if (!habilitado) return;
        try {
            Orden orden = ordenes.findById(evento.ordenId()).orElse(null);
            if (orden == null || orden.getCliente() == null || orden.getCliente().getCorreo() == null
                    || orden.getCliente().getCorreo().isBlank()) return;
            MimeMessage mensaje = correo.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            if (remitente != null && !remitente.isBlank()) helper.setFrom(remitente);
            helper.setTo(orden.getCliente().getCorreo());
            helper.setSubject("Luxury Perfumería - Pedido #" + orden.getId() + ": " + evento.estado());
            helper.setText("Hola " + orden.getCliente().getNombre() + ",\n\n"
                    + "El estado de tu pedido #" + orden.getId() + " cambió a: " + evento.estado() + ".\n"
                    + "Puedes revisar el seguimiento desde Mi cuenta > Mis pedidos.\n\n"
                    + "Luxury Perfumería", false);
            correo.send(mensaje);
            log.info("Aviso de seguimiento enviado para orden {}", orden.getId());
        } catch (Exception ex) {
            log.error("No se pudo enviar aviso de seguimiento para orden {}: {}", evento.ordenId(), ex.getMessage());
        }
    }
}
