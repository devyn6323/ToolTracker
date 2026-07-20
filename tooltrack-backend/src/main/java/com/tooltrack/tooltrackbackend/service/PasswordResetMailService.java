package com.tooltrack.tooltrackbackend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import com.tooltrack.tooltrackbackend.exception.ApiException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Service
public class PasswordResetMailService {
    private final RestClient restClient;
    private final String apiKey;
    private final String from;
    private final String fromName;
    private final boolean required;

    @Autowired
    public PasswordResetMailService(@Value("${app.sendgrid.api-key:}") String apiKey,
                                    @Value("${app.mail.from:}") String from,
                                    @Value("${app.mail.from-name:ToolTrack}") String fromName,
                                    @Value("${app.mail.required:false}") boolean required,
                                    @Value("${app.sendgrid.base-url:https://api.sendgrid.com}") String baseUrl) {
        this(createClient(baseUrl), apiKey, from, fromName, required);
    }

    PasswordResetMailService(RestClient restClient, String apiKey, String from, String fromName, boolean required) {
        this.restClient = restClient;
        this.apiKey = apiKey.trim();
        this.from = from.trim();
        this.fromName = fromName.trim();
        this.required = required;
    }

    @PostConstruct
    void validateProductionConfiguration() {
        if (required && (apiKey.isBlank() || from.isBlank())) {
            throw new IllegalStateException("SENDGRID_API_KEY and MAIL_FROM are required when password email is enabled");
        }
    }

    public void sendResetCode(String recipient, String name, String code) {
        if (apiKey.isBlank() || from.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Password email is not configured yet");
        }
        String text = "Hello " + name + ",\n\nYour ToolTrack password reset code is " + code
                + ". It expires in 15 minutes and can only be used once.\n\n"
                + "If you did not request this, you can ignore this email.";
        SendGridMessage message = new SendGridMessage(
                List.of(new Personalization(List.of(new EmailAddress(recipient, name)))),
                new EmailAddress(from, fromName),
                "Your ToolTrack password reset code",
                List.of(new MessageContent("text/plain", text)));
        try {
            restClient.post()
                    .uri("/v3/mail/send")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Password email could not be delivered. Please try again shortly");
        }
    }

    private static RestClient createClient(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder()
                .baseUrl(baseUrl.trim())
                .requestFactory(requestFactory)
                .build();
    }

    private record SendGridMessage(List<Personalization> personalizations, EmailAddress from,
                                   String subject, List<MessageContent> content) {
    }

    private record Personalization(List<EmailAddress> to) {
    }

    private record EmailAddress(String email, String name) {
    }

    private record MessageContent(String type, String value) {
    }
}
