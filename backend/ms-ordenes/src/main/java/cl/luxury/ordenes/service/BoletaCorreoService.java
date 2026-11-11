package cl.luxury.ordenes.service;

import cl.luxury.ordenes.model.BoletaEnvio;
import cl.luxury.ordenes.model.Orden;
import cl.luxury.ordenes.repository.BoletaEnvioRepository;
import cl.luxury.ordenes.repository.OrdenRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
public class BoletaCorreoService {
    private static final Logger log = LoggerFactory.getLogger(BoletaCorreoService.class);
    private static final Set<String> CONFIRMADAS = Set.of("Pagado", "En preparación", "Enviado", "Entregado");
    private final OrdenRepository ordenes;
    private final BoletaEnvioRepository envios;
    private final BoletaPdfService pdf;
    private final JavaMailSender correo;
    private final boolean habilitado;
    private final String remitente;
    private final String usuarioSmtp;
    private final String claveSmtp;
    private final boolean autenticacion;

    public BoletaCorreoService(OrdenRepository ordenes, BoletaEnvioRepository envios, BoletaPdfService pdf,
            JavaMailSender correo, @Value("${luxury.mail.enabled:false}") boolean habilitado,
            @Value("${luxury.mail.from:}") String remitente,
            @Value("${spring.mail.username:}") String usuarioSmtp,
            @Value("${spring.mail.password:}") String claveSmtp,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean autenticacion) {
        this.ordenes = ordenes; this.envios = envios; this.pdf = pdf; this.correo = correo;
        this.habilitado = habilitado; this.remitente = remitente; this.usuarioSmtp = usuarioSmtp;
        this.claveSmtp = claveSmtp; this.autenticacion = autenticacion;
    }

    // Después del commit, una transacción separada persiste la tarea antes de contactar SMTP.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(OrdenPagadaEvento evento) {
        try { preparar(evento.ordenId()); }
        catch (Exception ex) { log.error("No se pudo registrar el envío de boleta {} ({})", evento.ordenId(), ex.getClass().getSimpleName()); }
    }

    public Map<String, Object> consultar(Long id) {
        Orden orden = confirmarOrden(id);
        BoletaEnvio envio = envios.findById(id).orElse(null);
        String estado = envio == null ? "SIN_SOLICITAR" : envio.getEstado();
        String mensaje = envio == null ? "Puedes solicitar el envío o descargar tu comprobante PDF." : envio.getMensaje();
        return Map.of("estado", estado, "mensaje", mensaje == null ? "Preparando comprobante." : mensaje,
                "destinatario", orden.getCliente().getCorreo(), "intentos", envio == null ? 0 : envio.getIntentos());
    }

    @Transactional
    public Map<String, Object> solicitar(Long id) {
        preparar(id);
        return consultar(id);
    }

    private void preparar(Long id) {
        confirmarOrden(id);
        BoletaEnvio envio = envios.findById(id).orElseGet(() -> { BoletaEnvio e = new BoletaEnvio(); e.setOrdenId(id); return e; });
        if (Set.of("ENVIADO", "ENVIANDO").contains(envio.getEstado()) ||
                (envio.getIntentos() > 0 && "PENDIENTE".equals(envio.getEstado()))) return;
        envio.setActualizado(Instant.now());
        envio.setProximoIntento(Instant.now());
        envio.setIntentos(0);
        if (!configurado()) {
            envio.setEstado("DESACTIVADO");
            envio.setMensaje("El envío por correo no está configurado. Descarga tu PDF y contacta a la tienda.");
            log.warn("Boleta {}: revisar MAIL_ENABLED, SMTP_USER, SMTP_PASSWORD y MAIL_FROM en ms-ordenes.", id);
        } else {
            envio.setEstado("PENDIENTE");
            envio.setMensaje("Tu comprobante está en espera de envío al correo indicado en la compra.");
        }
        envios.saveAndFlush(envio);
    }

    private boolean configurado() {
        return habilitado && !remitente.isBlank() && (!autenticacion || (!usuarioSmtp.isBlank() && !claveSmtp.isBlank()));
    }

    private Orden confirmarOrden(Long id) {
        Orden orden = ordenes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la orden."));
        if (!CONFIRMADAS.contains(orden.getEstado())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo puedes enviar comprobantes de compras confirmadas.");
        return orden;
    }

    // Lote pequeño y espera entre reintentos: no bloquea checkout ni crea hilos por compra.
    @Scheduled(fixedDelayString = "${luxury.mail.poll-ms:15000}", initialDelayString = "${luxury.mail.initial-delay-ms:5000}")
    public void procesarPendientes() {
        envios.recuperarInterrumpidos(Instant.now().minusSeconds(300));
        for (BoletaEnvio envio : envios.findTop10ByEstadoAndProximoIntentoLessThanEqualOrderByProximoIntentoAsc("PENDIENTE", Instant.now())) {
            if (envios.reservar(envio.getOrdenId(), Instant.now()) != 1) continue;
            envio.setIntentos(envio.getIntentos() + 1);
            try {
                if (!configurado()) throw new IllegalStateException("SMTP sin configuración");
                Orden orden = confirmarOrden(envio.getOrdenId());
                MimeMessage mensaje = correo.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
                helper.setFrom(remitente); helper.setTo(orden.getCliente().getCorreo());
                helper.setSubject("Maison Luxury · Comprobante de compra #" + orden.getId());
                helper.setText("Hola " + orden.getCliente().getNombre() + ",\n\nAdjuntamos el comprobante PDF de tu compra #" + orden.getId()
                        + ".\n\nGracias por elegir Maison Luxury.", false);
                helper.addAttachment("boleta-maison-luxury-" + orden.getId() + ".pdf",
                        new org.springframework.core.io.ByteArrayResource(pdf.generar(orden)), "application/pdf");
                correo.send(mensaje);
                envio.setEstado("ENVIADO");
                envio.setMensaje("El servidor de correo aceptó tu comprobante. Revisa tu bandeja de entrada y spam.");
                log.info("Comprobante de orden {} aceptado por SMTP.", orden.getId());
            } catch (Exception ex) {
                boolean autenticacionFallida = ex instanceof MailAuthenticationException;
                boolean reintentar = configurado() && !autenticacionFallida && envio.getIntentos() < 3;
                envio.setEstado(reintentar ? "PENDIENTE" : "ERROR");
                envio.setMensaje(reintentar ? "El servidor de correo no respondió. Reintentaremos automáticamente."
                        : autenticacionFallida ? "El proveedor rechazó las credenciales de correo de la tienda. Puedes descargar tu PDF."
                        : "No se pudo enviar el correo. Puedes descargar tu PDF o solicitar un nuevo intento.");
                envio.setProximoIntento(Instant.now().plusSeconds(60L * envio.getIntentos()));
                log.warn("Boleta {}: intento {} falló ({}). Revisar conexión/configuración SMTP.", envio.getOrdenId(), envio.getIntentos(), ex.getClass().getSimpleName());
            }
            envio.setActualizado(Instant.now());
            envios.save(envio);
        }
    }
}
