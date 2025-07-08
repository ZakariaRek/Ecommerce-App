package com.Ecommerce.User_Service.security.oAuth2;

import com.Ecommerce.User_Service.security.jwt.JwtUtils;
import com.Ecommerce.User_Service.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${app.oauth2.authorizedRedirectUris:http://localhost:3000/oauth2/redirect}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect.");
            return;
        }

        String targetUrl = determineTargetUrl(request, response, authentication);
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {

        // Get OAuth2 user details
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Create UserDetailsImpl from OAuth2User for JWT generation
        UserDetailsImpl userDetails = UserDetailsImpl.buildFromOAuth2User(oAuth2User);

        // Generate JWT token
        String jwt = jwtUtils.generateJwtToken(authentication);

        // Build redirect URL with JWT token
        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", jwt)
                .queryParam("type", "oauth2")
                .build().toUriString();
    }
}