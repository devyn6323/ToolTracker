package com.tooltrack.tooltrackbackend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.tooltrack.tooltrackbackend.exception.ApiException;

@Service
public class PasswordResetMailService {
    private final JavaMailSender mailSender;
    private final String from;
    private final boolean required;
    private final String configuredHost;

    public PasswordResetMailService(ObjectProvider<JavaMailSender> mailSender,
                                    @Value("${app.mail.from:}") String from,
                                    @Value("${app.mail.required:false}") boolean required,
                                    @Value("${spring.mail.host:}") String configuredHost) {
        this.mailSender = mailSender.getIfAvailable();
        this.from = from.trim();
        this.required = required;
        this.configuredHost = configuredHost.trim();
    }

    @PostConstruct
    void validateProductionConfiguration() {
        if (required && (mailSender == null || configuredHost.isBlank() || from.isBlank())) {
            throw new IllegalStateException("SMTP_HOST and MAIL_FROM are required when password email is enabled");
        }
    }

    public void sendResetCode(String recipient, String name, String code) {
        if (mailSender == null || configuredHost.isBlank() || from.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Password email is not configured yet");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Your ToolTrack password reset code");
        message.setText("Hello " + name + ",\n\nYour ToolTrack password reset code is " + code
                + ". It expires in 15 minutes and can only be used once.\n\n"
                + "If you did not request this, you can ignore this email.");
        mailSender.send(message);
    }
}
