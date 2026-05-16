package com.bookcollection.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    @Test
    void generateToken_shouldContainUserIdAndUsernameClaims() {
        String token = JwtUtils.generateToken(123L, "alice");

        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = JwtUtils.parseToken(token);
        assertEquals(123L, claims.get("userId", Long.class));
        assertEquals("alice", claims.get("username", String.class));
        assertEquals("alice", claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void parseToken_shouldSupportBearerPrefixAndTrim() {
        String rawToken = JwtUtils.generateToken(7L, "bob");
        String bearerToken = "  Bearer   " + rawToken + "  ";

        Claims claims = JwtUtils.parseToken(bearerToken);
        assertEquals(7L, claims.get("userId", Long.class));
        assertEquals("bob", claims.get("username", String.class));
    }

    @Test
    void getUserId_and_getUsername_shouldWork() {
        String token = JwtUtils.generateToken(42L, "carol");

        assertEquals(42L, JwtUtils.getUserId(token));
        assertEquals("carol", JwtUtils.getUsername(token));
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertFalse(JwtUtils.validateToken("not-a-jwt"));

        String token = JwtUtils.generateToken(1L, "dave");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
        String signature = parts[2];
        char last = signature.charAt(signature.length() - 1);
        char replaced = last == 'a' ? 'b' : 'a';
        String tamperedSignature = signature.substring(0, signature.length() - 1) + replaced;
        String tamperedToken = parts[0] + "." + parts[1] + "." + tamperedSignature;

        assertFalse(JwtUtils.validateToken(tamperedToken));
    }
}
