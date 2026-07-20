package com.tooltrack.tooltrackbackend.service;

import com.tooltrack.tooltrackbackend.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class PasswordResetMailServiceTests {
    @Test
    void sendsResetCodeThroughBrevoHttpsApi() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.brevo.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PasswordResetMailService service = new PasswordResetMailService(
                builder.build(), "brevo-test-key", "support@example.com", "ToolTrack", true);

        server.expect(once(), requestTo("https://api.brevo.test/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "brevo-test-key"))
                .andExpect(content().json("""
                        {
                          "sender":{"email":"support@example.com","name":"ToolTrack"},
                          "to":[{"email":"owner@example.com","name":"Demo Owner"}],
                          "subject":"Your ToolTrack password reset code",
                          "textContent":"Hello Demo Owner,\\n\\nYour ToolTrack password reset code is 12345678. It expires in 15 minutes and can only be used once.\\n\\nIf you did not request this, you can ignore this email."
                        }
                        """))
                .andRespond(withStatus(HttpStatus.CREATED));

        service.sendResetCode("owner@example.com", "Demo Owner", "12345678");
        server.verify();
    }

    @Test
    void convertsBrevoRejectionIntoTemporaryServiceError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.brevo.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PasswordResetMailService service = new PasswordResetMailService(
                builder.build(), "brevo-invalid", "support@example.com", "ToolTrack", true);
        server.expect(requestTo("https://api.brevo.test/v3/smtp/email"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> service.sendResetCode("owner@example.com", "Demo Owner", "12345678"))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        server.verify();
    }
}
