package com.ecommerce.messaging_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text email notifications via the configured SMTP server
 * (MailHog in local dev). Email delivery is best-effort: failures are logged
 * but never allowed to break the surrounding application logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@ecommerce.local}")
    private String from;

    public void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Skipping email with no recipient (subject '{}')", subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Sent '{}' to {}", subject, to);
        } catch (MessagingException ex) {
            log.error("Failed to send '{}' to {}: {}", subject, to, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Failed to deliver '{}' to {}: {}", subject, to, ex.getMessage());
        }
    }
}
