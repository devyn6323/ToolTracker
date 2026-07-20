package com.tooltrack.tooltrackbackend.security;

import com.tooltrack.tooltrackbackend.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration}") long expirationMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public String createToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("companyId", user.getCompany().getId().toString())
                .claim("role", user.getRole().name())
                .claim("sessionVersion", user.getSessionVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(key)
                .compact();
    }

    public JwtIdentity parseIdentity(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Integer sessionVersion = claims.get("sessionVersion", Integer.class);
        return new JwtIdentity(UUID.fromString(claims.getSubject()), sessionVersion == null ? 0 : sessionVersion);
    }

    public record JwtIdentity(UUID userId, int sessionVersion) {
    }
}
