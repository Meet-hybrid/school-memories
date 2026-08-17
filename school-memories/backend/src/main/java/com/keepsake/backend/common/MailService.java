package com.keepsake.backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Outbound email. When {@code keepsake.mail.enabled} is true, links are sent via
 * the configured SMTP server (spring.mail.*); otherwise they are logged to the
 * console and returned by the API so the flows still work in development.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final boolean enabled;
    private final JavaMailSender mailSender;
    private final String from;
    private final String appBaseUrl;

    public MailService(@Value("${keepsake.mail.enabled:false}") boolean enabled,
                       @Value("${keepsake.mail.from:}") String from,
                       @Value("${keepsake.app.base-url:http://localhost:3000}") String appBaseUrl,
                       JavaMailSender mailSender) {
        this.enabled = enabled;
        this.mailSender = mailSender;
        this.from = (from == null || from.isBlank()) ? "Keepsake <no-reply@keepsake.local>" : from.trim();
        this.appBaseUrl = appBaseUrl == null ? "http://localhost:3000" : appBaseUrl.replaceAll("/+$", "");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void sendVerificationLink(String email, String link) {
        String url = absolute(link);
        if (enabled) {
            send(email, "Verify your email on Keepsake",
                    "<p>Welcome to Keepsake — the school memory community. Confirm this is your email "
                            + "to finish creating your account.</p>"
                            + "<p style=\"margin:24px 0\"><a href=\"" + url + "\" "
                            + "style=\"background:#2f3b34;color:#faf7f0;padding:10px 18px;border-radius:999px;"
                            + "text-decoration:none;font-size:14px\">Verify my email</a></p>"
                            + "<p style=\"font-size:13px;color:#6b6b63\">If the button doesn't work, copy this link: "
                            + "<a href=\"" + url + "\">" + url + "</a></p>");
        } else {
            log.info("===== EMAIL VERIFICATION (mail disabled, log only) =====\nTo: {}\nLink: {}\n===========================================",
                    email, url);
        }
    }

    public void sendPasswordResetLink(String email, String link) {
        String url = absolute(link);
        if (enabled) {
            send(email, "Reset your Keepsake password",
                    "<p>We got a request to reset your Keepsake password. This link expires in one hour.</p>"
                            + "<p style=\"margin:24px 0\"><a href=\"" + url + "\" "
                            + "style=\"background:#2f3b34;color:#faf7f0;padding:10px 18px;border-radius:999px;"
                            + "text-decoration:none;font-size:14px\">Reset my password</a></p>"
                            + "<p style=\"font-size:13px;color:#6b6b63\">If the button doesn't work, copy this link: "
                            + "<a href=\"" + url + "\">" + url + "</a><br/><br/>"
                            + "If you didn't ask for this, you can safely ignore this email.</p>");
        } else {
            log.info("===== PASSWORD RESET (mail disabled, log only) =====\nTo: {}\nLink: {}\n===========================================",
                    email, url);
        }
    }

    private String absolute(String link) {
        return link != null && link.startsWith("/") ? appBaseUrl + link : link;
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(wrap(htmlBody), true);
            mailSender.send(message);
            log.info("Sent email '{}' to {}", subject, to);
        } catch (Exception ex) {
            // Never break a signup/reset because mail failed — the operator sees the error.
            log.error("Failed to send email to {}", to, ex);
        }
    }

    private String wrap(String body) {
        return "<div style=\"background:#faf7f0;padding:32px 16px;font-family:Georgia,'Times New Roman',serif;color:#2f3b34\">"
                + "<div style=\"max-width:520px;margin:0 auto;background:#fffdf8;border:1px solid #e8e2d4;"
                + "border-radius:12px;padding:32px;font-family:Georgia,serif\">"
                + "<p style=\"font-size:20px;margin:0 0 16px;letter-spacing:0.02em\">Keepsake</p>"
                + "<div style=\"height:1px;background:#e8e2d4;margin-bottom:20px\"></div>"
                + "<div style=\"font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;font-size:15px;line-height:1.6\">"
                + body + "</div></div></div>";
    }
}
