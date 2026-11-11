package cl.luxury.catalogo.service;

import cl.luxury.catalogo.model.AlertaStock;
import cl.luxury.catalogo.model.Producto;
import cl.luxury.catalogo.repository.AlertaStockRepository;
import cl.luxury.catalogo.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@Service
public class AlertaStockService {
    private static final Logger log = LoggerFactory.getLogger(AlertaStockService.class);
    private final AlertaStockRepository alertas;
    private final ProductoRepository productos;
    private final JavaMailSender correo;
    private final ApplicationEventPublisher eventos;
    private final boolean habilitado;
    private final String remitente;
    private final org.springframework.transaction.support.TransactionTemplate transacciones;

    public AlertaStockService(AlertaStockRepository alertas, ProductoRepository productos,
            JavaMailSender correo, ApplicationEventPublisher eventos,
            org.springframework.transaction.PlatformTransactionManager gestor,
            @Value("${luxury.mail.enabled:false}") boolean habilitado,
            @Value("${luxury.mail.from:}") String remitente) {
        this.alertas = alertas; this.productos = productos; this.correo = correo;
        this.eventos = eventos; this.transacciones = new org.springframework.transaction.support.TransactionTemplate(gestor); this.habilitado = habilitado; this.remitente = remitente;
    }

    @Transactional
    public Map<String, String> solicitar(Long id, String email) {
        Producto producto = productos.findById(id).orElseThrow(() -> new IllegalArgumentException("Producto inexistente"));
        if (producto.getStock() != null && producto.getStock() > 0)
            throw new IllegalArgumentException("Este producto ya está disponible");
        String destinatario = email == null ? "" : email.trim().toLowerCase();
        if (destinatario.length() > 254 || !destinatario.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Correo electrónico inválido");
        if (alertas.existsByProductoIdAndCorreoIgnoreCaseAndEnviadaFalse(id, destinatario))
            return Map.of("mensaje", "Ya tienes un aviso pendiente para este producto.");
        AlertaStock alerta = new AlertaStock(); alerta.setProductoId(id); alerta.setCorreo(destinatario);
        alertas.save(alerta);
        return Map.of("mensaje", "Te avisaremos por correo cuando vuelva el stock.");
    }

    public void avisarReposicion(Long productoId) { eventos.publishEvent(new StockRepuestoEvento(productoId)); }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarPendientes(StockRepuestoEvento evento) {
        if (!habilitado) return;
        Producto producto = productos.findById(evento.productoId()).orElse(null);
        if (producto == null || producto.getStock() == null || producto.getStock() <= 0) return;
        for (AlertaStock alerta : alertas.findByProductoIdAndEnviadaFalse(evento.productoId())) {
            try {
                SimpleMailMessage mensaje = new SimpleMailMessage();
                if (remitente != null && !remitente.isBlank()) mensaje.setFrom(remitente);
                mensaje.setTo(alerta.getCorreo());
                mensaje.setSubject("Luxury Perfumería - ¡Volvió " + producto.getNombre() + "!");
                mensaje.setText("Hola,\n\nEl perfume " + producto.getNombre()
                        + " vuelve a estar disponible en Luxury Perfumería.\n"
                        + "Visítanos para revisar su disponibilidad.\n\nLuxury Perfumería");
                correo.send(mensaje);
                transacciones.executeWithoutResult(status -> {
                    alertas.findById(alerta.getId()).ifPresent(a -> { a.setEnviada(true); alertas.save(a); });
                });
            } catch (Exception e) {
                log.warn("No se pudo enviar alerta de stock {}: {}", alerta.getId(), e.getMessage());
            }
        }
    }

}
