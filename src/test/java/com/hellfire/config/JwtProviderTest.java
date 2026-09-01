package com.hellfire.config;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {

    private static final String SECRET = "test-only-secret-0123456789-abcdefghijklmnopqrstuvwxyz-0123456789";

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, 3_600_000L);
    }

    private Authentication auth(String email, String... roles) {
        return new UsernamePasswordAuthenticationToken(email, null,
                AuthorityUtils.createAuthorityList(roles));
    }

    @Test
    void generatedTokenRoundTripsEmail() {
        String token = jwtProvider.generateToken(auth("user@example.com", "CUSTOMER"));
        assertEquals("user@example.com", jwtProvider.getEmailFromJwtToken(token));
    }

    @Test
    void acceptsTokenWithBearerPrefix() {
        String token = jwtProvider.generateToken(auth("user@example.com", "CUSTOMER"));
        assertEquals("user@example.com", jwtProvider.getEmailFromJwtToken("Bearer " + token));
    }

    @Test
    void carriesAuthoritiesClaim() {
        String token = jwtProvider.generateToken(auth("owner@example.com", "ADMIN"));
        assertEquals("ADMIN", jwtProvider.parseToken(token).get("authorities", String.class));
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtProvider other = new JwtProvider(SECRET.replace('t', 'x'), 3_600_000L);
        String token = other.generateToken(auth("user@example.com", "CUSTOMER"));
        assertThrows(JwtException.class, () -> jwtProvider.getEmailFromJwtToken(token));
    }

    @Test
    void rejectsExpiredToken() {
        JwtProvider shortLived = new JwtProvider(SECRET, -1000L);
        String token = shortLived.generateToken(auth("user@example.com", "CUSTOMER"));
        assertThrows(JwtException.class, () -> jwtProvider.getEmailFromJwtToken(token));
    }

    @Test
    void rejectsGarbageToken() {
        assertThrows(JwtException.class, () -> jwtProvider.getEmailFromJwtToken("Bearer not-a-jwt"));
    }
}
