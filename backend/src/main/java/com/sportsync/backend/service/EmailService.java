package com.sportsync.backend.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envío de correos transaccionales (HTML).
 * Si no hay SMTP configurado (MAIL_USERNAME vacío) y el modo dev-log está activo,
 * el contenido se imprime en consola en lugar de enviarse — así el flujo se puede
 * probar sin credenciales reales.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${sportsync.verificacion.from}")
    private String from;

    @Value("${spring.mail.username:}")
    private String smtpUser;

    @Value("${sportsync.verificacion.modo-dev-log:true}")
    private boolean modoDevLog;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private boolean smtpConfigurado() {
        return smtpUser != null && !smtpUser.isBlank();
    }

    public void enviarCodigoVerificacion(String destino, String codigo, int expiracionMinutos) {
        String asunto = "Tu código de verificación de SportSync";

        if (!smtpConfigurado()) {
            if (modoDevLog) {
                System.out.println("========================================");
                System.out.println("[EMAIL DEV-LOG] Para: " + destino);
                System.out.println("[EMAIL DEV-LOG] Código: " + codigo + " (vence en " + expiracionMinutos + " min)");
                System.out.println("========================================");
                return;
            }
            throw new IllegalStateException("SMTP no configurado: definí MAIL_USERNAME/MAIL_PASSWORD.");
        }

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(from);
            helper.setTo(destino);
            helper.setSubject(asunto);
            helper.setText(htmlCodigo(codigo, expiracionMinutos), true); // true = HTML
            mailSender.send(mime);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el email de verificación: " + e.getMessage(), e);
        }
    }

    /**
     * Plantilla HTML con la identidad visual de SportSync (navy #162338, azul #2f66e8).
     * Usa estilos inline porque los clientes de correo ignoran el CSS externo.
     */
    private String htmlCodigo(String codigo, int expiracionMinutos) {
        return """
            <div style="margin:0;padding:24px;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;">
              <div style="max-width:480px;margin:0 auto;background:#ffffff;border-radius:14px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">

                <div style="background:#162338;padding:22px 28px;">
                  <span style="color:#ffffff;font-size:22px;font-weight:800;letter-spacing:0.3px;">SportSync</span>
                </div>

                <div style="padding:32px 28px;">
                  <h1 style="margin:0 0 8px;font-size:20px;color:#1b1f23;">Verificá tu cuenta</h1>
                  <p style="margin:0 0 24px;font-size:14px;line-height:1.5;color:#5f6773;">
                    Ingresá este código en la pestaña de verificación para activar tu cuenta:
                  </p>

                  <div style="text-align:center;margin:0 0 22px;">
                    <div style="display:inline-block;background:#dfeafc;border:2px solid #2f66e8;border-radius:12px;
                                padding:16px 28px;font-size:34px;font-weight:800;letter-spacing:10px;color:#234fc0;">
                      %s
                    </div>
                  </div>

                  <p style="margin:0;text-align:center;font-size:13px;color:#5f6773;">
                    El código vence en <strong>%d minutos</strong>.
                  </p>
                </div>

                <div style="border-top:1px solid #e2e5ea;padding:18px 28px;background:#f7f7f8;">
                  <p style="margin:0;font-size:12px;line-height:1.5;color:#9aa1ad;">
                    Si no te registraste en SportSync, ignorá este email.
                  </p>
                </div>

              </div>
            </div>
            """.formatted(codigo, expiracionMinutos);
    }
}
