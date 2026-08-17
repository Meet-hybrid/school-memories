package com.keepsake.backend.common;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailServiceTest {

    private final JavaMailSender sender = mock(JavaMailSender.class);
    private MimeMessage captured;

    @BeforeEach
    void setUp() {
        captured = null;
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doAnswer(inv -> {
            captured = inv.getArgument(0);
            return null;
        }).when(sender).send(any(MimeMessage.class));
    }

    private MailService service(boolean enabled) {
        return new MailService(enabled, "Keepsake <no-reply@keepsake.app>", "https://keepsake.example.com", sender);
    }

    @Test
    void sends_verification_email_with_absolute_link() throws Exception {
        service(true).sendVerificationLink("ada@example.dev", "/verify-email?token=abc123");

        verify(sender).send(any(MimeMessage.class));
        assertThat(captured).isNotNull();
        assertThat(captured.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("ada@example.dev");
        assertThat(captured.getSubject()).contains("Verify");
        assertThat(captured.getFrom()[0].toString()).contains("no-reply@keepsake.app");
        assertThat(bodyText(captured)).contains("https://keepsake.example.com/verify-email?token=abc123");
    }

    @Test
    void sends_password_reset_email() throws Exception {
        service(true).sendPasswordResetLink("bisi@example.dev", "/reset-password?token=xyz");

        verify(sender).send(any(MimeMessage.class));
        assertThat(captured.getSubject()).contains("Reset");
        assertThat(bodyText(captured)).contains("https://keepsake.example.com/reset-password?token=xyz");
    }

    @Test
    void disabled_mode_never_touches_the_sender() {
        MailService svc = service(false);
        assertThat(svc.isEnabled()).isFalse();

        svc.sendVerificationLink("ada@example.dev", "/verify-email?token=abc");
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void absolute_links_are_left_untouched() throws Exception {
        service(true).sendVerificationLink("ada@example.dev", "https://custom.domain/verify-email?token=abc");

        assertThat(bodyText(captured)).contains("https://custom.domain/verify-email?token=abc");
    }

    private static String bodyText(MimeMessage message) throws Exception {
        return extractText(message.getContent());
    }

    private static String extractText(Object content) throws Exception {
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof jakarta.mail.Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String text = extractText(multipart.getBodyPart(i).getContent());
                if (text != null && !text.isEmpty()) {
                    return text;
                }
            }
        }
        return null;
    }
}
