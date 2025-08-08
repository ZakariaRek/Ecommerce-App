package com.Ecommerce.Gateway_Service.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String testSecret = "c03a546beee68b92784e681b537540349c386d02b6dbf9917cf438e47e5c1ee93fdebc55652af00cb3ebc6bff17dc3bedaa33ea6cfdd1959b114ede448c4ac87853021cc3c32f1ef6d5951d0c6b1398bc01c563c7638a0000e6b4064c5733c5552aa232aa8547be8b4b1f8dddacac8256f319acd6832ff5ae9365358e20624fc99dab8489d33e582cf621444e9d944442559707a1f92d556862bb53ce12deb3ec17d3a8bc3c7159b672e4f02189af368a8e71d8547a5b71518de7a1d9a4997d20b4f646fae73e73c26666799b21cdec5544b74319756bb0a27d4e124ec5f13bf8f338ce3ba5ad8b3af1a8aae211bef3eb6ca4f8a24e6b80662c94530e168b0dc";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", testSecret);
    }

    @Test
    void shouldExtractUsernameFromValidToken() {
        // Given
        String token = createValidToken("testuser", List.of("ROLE_USER"));

        // When
        String username = jwtUtil.extractUsername(token);

        // Then
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void shouldExtractRolesFromValidToken() {
        // Given
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
        String token = createValidToken("testuser", roles);

        // When
        List<String> extractedRoles = jwtUtil.extractRoles(token);

        // Then
        assertThat(extractedRoles).containsExactlyElementsOf(roles);
    }

    @Test
    void shouldValidateValidToken() {
        // Given
        String token = createValidToken("testuser", List.of("ROLE_USER"));

        // When
        Boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectExpiredToken() {
        // Given
        String expiredToken = createExpiredToken("testuser", List.of("ROLE_USER"));

        // When
        Boolean isValid = jwtUtil.validateToken(expiredToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        Boolean isValid = jwtUtil.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    private String createValidToken(String username, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes());
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .signWith(key)
                .compact();
    }

    private String createExpiredToken(String username, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes());
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(key)
                .compact();
    }
}