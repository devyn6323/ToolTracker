package com.tooltrack.tooltrackbackend.service;

import com.tooltrack.tooltrackbackend.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
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
    void sendsResetCodeThroughSendGridHttpsApi() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.sendgrid.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PasswordResetMailService service = new PasswordResetMailService(
                builder.build(), "SG.test-key", "support@example.com", "ToolTrack", true);

        server.expect(once(), requestTo("https://api.sendgrid.test/v3/mail/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer SG.test-key"))
                .andExpect(content().json("""
                        {
                          "personalizations":[{"to":[{"email":"owner@example.com","name":"Demo Owner"}]}],
                          "from":{"email":"support@example.com","name":"ToolTrack"},
                          "subject":"Your ToolTrack password reset code",
                          "content":[{"type":"text/plain","value":"Hello Demo Owner,\\n\\nYour ToolTrack password reset code is 12345678. It expires in 15 minutes and can only be used once.\\n\\nIf you did not request this, you can ignore this email."}]
                        }
                        """))
                .andRespond(withStatus(HttpStatus.ACCEPTED));

        service.sendResetCode("owner@example.com", "Demo Owner", "12345678");
        server.verify();
    }

    @Test
    void convertsSendGridRejectionIntoTemporaryServiceError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.sendgrid.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PasswordResetMailService service = new PasswordResetMailService(
                builder.build(), "SG.invalid", "support@example.com", "ToolTrack", true);
        server.expect(requestTo("https://api.sendgrid.test/v3/mail/send"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> service.sendResetCode("owner@example.com", "Demo Owner", "12345678"))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        server.verify();
    }
}
