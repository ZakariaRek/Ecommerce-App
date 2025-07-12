package com.Ecommerce.User_Service.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OAuth2Controller {

    @GetMapping("/google")
    public void redirectToGoogle(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorize/google");
    }

    @GetMapping("/providers")
    public ResponseEntity<?> getAvailableProviders() {
        Map<String, Object> providers = new HashMap<>();
        providers.put("google", Map.of(
                "name", "Google",
                "url", "/oauth2/authorize/google",
                "enabled", true
        ));

        return ResponseEntity.ok(providers);
    }
}