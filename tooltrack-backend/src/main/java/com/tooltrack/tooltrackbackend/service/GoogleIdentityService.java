package com.tooltrack.tooltrackbackend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tooltrack.tooltrackbackend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Locale;

@Service
public class GoogleIdentityService {
    private final String webClientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdentityService(@Value("${app.google.web-client-id:}") String webClientId) {
        this.webClientId = webClientId.trim();
        this.verifier = this.webClientId.isEmpty() ? null : new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(this.webClientId))
                .build();
    }

    public GoogleIdentity verify(String rawIdToken) {
        if (verifier == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured yet");
        }
        try {
            GoogleIdToken token = verifier.verify(rawIdToken);
            if (token == null || !Boolean.TRUE.equals(token.getPayload().getEmailVerified())) {
                throw invalidToken();
            }
            GoogleIdToken.Payload payload = token.getPayload();
            String email = payload.getEmail();
            String subject = payload.getSubject();
            if (email == null || email.isBlank() || subject == null || subject.isBlank()) {
                throw invalidToken();
            }
            int atSign = email.indexOf('@');
            String emailName = atSign > 0 ? email.substring(0, atSign) : "ToolTrack user";
            Object nameClaim = payload.get("name");
            String name = nameClaim instanceof String value && !value.isBlank()
                    ? value.trim() : emailName;
            return new GoogleIdentity(subject, email.trim().toLowerCase(Locale.ROOT), name);
        } catch (GeneralSecurityException | IOException exception) {
            throw invalidToken();
        }
    }

    private ApiException invalidToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Google sign-in could not be verified");
    }

    public record GoogleIdentity(String subject, String email, String name) {
    }
}
