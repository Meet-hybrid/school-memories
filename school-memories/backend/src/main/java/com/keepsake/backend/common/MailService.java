package com.keepsake.backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Outbound email. The MVP has no SMTP configured, so links are logged to the
 * console (and returned by the API in dev mode). A real mail sender can be
 * dropped in behind this class without touching callers.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final boolean enabled;

    public MailService(@Value("${keepsake.mail.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public void sendVerificationLink(String email, String link) {
        if (enabled) {
            log.info("[mail] verification link for {}: {}", email, link);
        } else {
            log.info("===== EMAIL VERIFICATION (mail disabled, log only) =====\nTo: {}\nLink: {}\n===========================================", email, link);
        }
    }

    public void sendPasswordResetLink(String email, String link) {
        if (enabled) {
            log.info("[mail] password reset link for {}: {}", email, link);
        } else {
            log.info("===== PASSWORD RESET (mail disabled, log only) =====\nTo: {}\nLink: {}\n===========================================", email, link);
        }
    }
}
