package cl.luxury.usuarios.correo;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class CorreoBienvenidaService {
    private static final Logger log = LoggerFactory.getLogger(CorreoBienvenidaService.class);
    private final JavaMailSender correo;
    private final boolean habilitado;
    private final String remitente;

    public CorreoBienvenidaService(JavaMailSender correo,
            @Value("${luxury.mail.enabled:false}") boolean habilitado,
            @Value("${luxury.mail.from:}") String remitente) {
        this.correo = correo;
        this.habilitado = habilitado;
        this.remitente = remitente;
    }

    @Async("bienvenidaExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrar(RegistroCreadoEvent evento) {
        if (!habilitado || remitente.isBlank()) {
            log.debug("Correo de bienvenida desactivado");
            return;
        }
        try {
            MimeMessage mensaje = correo.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(evento.correo());
            helper.setSubject("¡Bienvenido a Luxury Perfumería!");
            String nombre = escaparHtml(evento.nombre());
            String html = "<div style='background:#101010;padding:32px;font-family:Arial,sans-serif;color:#f5f1e8'>"
                    + "<div style='max-width:580px;margin:auto;border:1px solid #b99752;padding:30px'>"
                    + "<h1 style='color:#d6b46c;letter-spacing:2px'>LUXURY PERFUMERÍA</h1>"
                    + "<h2 style='color:#ffffff'>¡Bienvenido a Luxury!</h2>"
                    + "<p>Hola, " + nombre + ":</p>"
                    + "<p>Tu cuenta se ha creado correctamente. Ya puedes explorar nuestras fragancias, "
                    + "guardar favoritos y realizar compras.</p>"
                    + "<p>¡Gracias por formar parte de Luxury Perfumería!</p>"
                    + "<p style='color:#d6b46c'>La elegancia comienza con una fragancia.</p>"
                    + "</div></div>";
            helper.setText(html, true);
            correo.send(mensaje);
            log.info("Correo de bienvenida enviado a {}", evento.correo());
        } catch (Exception e) {
            // Nunca debe invalidar la cuenta ya guardada. No registrar credenciales.
            log.warn("No se pudo enviar correo de bienvenida a {}: {}", evento.correo(), e.getMessage());
        }
    }

    private static String escaparHtml(String value) {
        if (value == null) return "Cliente";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
