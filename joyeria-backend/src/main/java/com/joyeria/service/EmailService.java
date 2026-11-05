package com.joyeria.service;

import com.joyeria.event.OrderCreatedEvent;
import com.joyeria.model.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${email.from}")
    private String from;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.log-link:false}")
    private boolean logLink;

    public boolean isConfigured() {
        return emailEnabled && mailHost != null && !mailHost.isBlank();
    }

    public void sendPasswordReset(User user, String token) {
        String link = frontendUrl + "/restablecer-contrasena?token=" + token;
        if (!isConfigured()) {
            fallback("restablecimiento de contraseña", user.getEmail(), link);
            return;
        }
        String html = """
                <h2>Recupera tu contraseña</h2>
                <p>Hola %s,</p>
                <p>Recibimos una solicitud para restablecer tu contraseña. El enlace expira en 2 horas.</p>
                <p><a href="%s">Restablecer contraseña</a></p>
                <p>Si no solicitaste esto, puedes ignorar este correo.</p>
                """.formatted(user.getFirstName(), link);
        send(user.getEmail(), "Recupera tu contraseña", html);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        String link = frontendUrl + "/pedido-confirmado/" + event.orderNumber();
        if (!isConfigured()) {
            fallback("confirmación del pedido " + event.orderNumber(), event.email(), link);
            return;
        }
        String html = """
                <h2>¡Gracias por tu compra!</h2>
                <p>Hola %s,</p>
                <p>Tu pedido <strong>#%s</strong> fue registrado por un total de %s.</p>
                <p><a href="%s">Ver el estado de tu pedido</a></p>
                """.formatted(event.firstName(), event.orderNumber(), event.total(), link);
        send(event.email(), "Confirmación de pedido #" + event.orderNumber(), html);
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email '{}' enviado a {}", subject, to);
        } catch (Exception e) {
            log.error("No se pudo enviar el email '{}' a {}: {}", subject, to, e.getMessage());
        }
    }

    private void fallback(String kind, String email, String link) {
        if (logLink) {
            log.warn("EMAIL no configurado (modo log). Enlace de {} para {}: {}", kind, email, link);
        } else {
            log.warn("EMAIL no configurado: no se pudo enviar el correo de {}. Destinatario: {}. "
                    + "Configura EMAIL_ENABLED=true y spring.mail.* (o EMAIL_LOG_LINK=true en desarrollo).",
                    kind, email);
        }
    }
}
