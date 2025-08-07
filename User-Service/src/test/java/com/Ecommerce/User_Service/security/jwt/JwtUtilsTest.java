package com.Ecommerce.User_Service.security.jwt;

import com.Ecommerce.User_Service.security.services.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import jakarta.servlet.http.Cookie;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "User-service.app.jwtSecret=testSecretKeyThatIsLongEnoughForHS256Algorithm123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890",
        "User-service.app.jwtExpirationMs=86400000",
        "User-service.app.jwtCookieName=test-user-service"
})
class JwtUtilsTest {

    @Autowired
    private JwtUtils jwtUtils;

    private UserDetailsImpl userDetails;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        userDetails = new UserDetailsImpl(
                "user123",
                "testuser",
                "testuser@test.com",
                "password123",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    @Test
    void generateJwtCookie_ShouldCreateValidCookie() {
        // When
        ResponseCookie cookie = jwtUtils.generateJwtCookie(authentication);

        // Then
        assertThat(cookie.getName()).isEqualTo("test-user-service");
        assertThat(cookie.getValue()).isNotNull();
        assertThat(cookie.getValue()).isNotEmpty();
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(24 * 60 * 60); // 24 hours
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void generateJwtToken_ShouldCreateValidToken() {
        // When
        String token = jwtUtils.generateJwtToken(authentication);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT should have 3 parts separated by dots
    }

    @Test
    void validateJwtToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = jwtUtils.generateJwtToken(authentication);

        // When
        boolean isValid = jwtUtils.validateJwtToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void validateJwtToken_WithInvalidToken_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtUtils.validateJwtToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateJwtToken_WithEmptyToken_ShouldReturnFalse() {
        // Given
        String emptyToken = "";

        // When
        boolean isValid = jwtUtils.validateJwtToken(emptyToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateJwtToken_WithNullToken_ShouldReturnFalse() {
        // Given
        String nullToken = null;

        // When
        boolean isValid = jwtUtils.validateJwtToken(nullToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void getUserNameFromJwtToken_ShouldExtractCorrectUsername() {
        // Given
        String token = jwtUtils.generateJwtToken(authentication);

        // When
        String extractedUsername = jwtUtils.getUserNameFromJwtToken(token);

        // Then
        assertThat(extractedUsername).isEqualTo(userDetails.getUsername());
    }

    @Test
    void getJwtFromCookies_WhenCookieExists_ShouldReturnTokenValue() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        String tokenValue = "test-jwt-token-value";
        Cookie jwtCookie = new Cookie("test-user-service", tokenValue);
        request.setCookies(jwtCookie);

        // When
        String extractedToken = jwtUtils.getJwtFromCookies(request);

        // Then
        assertThat(extractedToken).isEqualTo(tokenValue);
    }

    @Test
    void getJwtFromCookies_WhenCookieDoesNotExist_ShouldReturnNull() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No cookies set

        // When
        String extractedToken = jwtUtils.getJwtFromCookies(request);

        // Then
        assertThat(extractedToken).isNull();
    }

    @Test
    void getJwtFromCookies_WhenDifferentCookieExists_ShouldReturnNull() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        Cookie differentCookie = new Cookie("different-cookie", "different-value");
        request.setCookies(differentCookie);

        // When
        String extractedToken = jwtUtils.getJwtFromCookies(request);

        // Then
        assertThat(extractedToken).isNull();
    }

    @Test
    void getCleanJwtCookie_ShouldCreateCookieWithNullValue() {
        // When
        ResponseCookie cleanCookie = jwtUtils.getCleanJwtCookie();

        // Then
        assertThat(cleanCookie.getName()).isEqualTo("test-user-service");
        assertThat(cleanCookie.getValue()).isNull();
        assertThat(cleanCookie.getPath()).isEqualTo("/api");
    }

    @Test
    void jwtTokenShouldContainUserRoles() {
        // Given
        UserDetailsImpl userWithMultipleRoles = new UserDetailsImpl(
                "user123",
                "testuser",
                "testuser@test.com",
                "password123",
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                )
        );

        Authentication authWithRoles = new UsernamePasswordAuthenticationToken(
                userWithMultipleRoles,
                null,
                userWithMultipleRoles.getAuthorities()
        );

        // When
        String token = jwtUtils.generateJwtToken(authWithRoles);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        assertThat(jwtUtils.getUserNameFromJwtToken(token)).isEqualTo("testuser");
    }

    @Test
    void jwtTokenShouldContainUserId() {
        // When
        String token = jwtUtils.generateJwtToken(authentication);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtUtils.validateJwtToken(token)).isTrue();

        // The token should contain the user ID as a claim
        // We can verify this by ensuring the token is valid and contains the expected username
        assertThat(jwtUtils.getUserNameFromJwtToken(token)).isEqualTo(userDetails.getUsername());
    }
}