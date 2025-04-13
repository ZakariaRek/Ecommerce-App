package com.Ecommerce.Gateway_Service.Controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SwaggerResourceController {

    @GetMapping("/swagger-resources")
    public ResponseEntity<String> getSwaggerResources() {
        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Ecommerce API Documentation</title>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; margin: 20px; }\n" +
                "        h1 { color: #333; }\n" +
                "        ul { list-style-type: none; padding: 0; }\n" +
                "        li { margin: 10px 0; }\n" +
                "        a { color: #0066cc; text-decoration: none; padding: 10px; display: inline-block; background-color: #f5f5f5; border-radius: 5px; }\n" +
                "        a:hover { background-color: #e0e0e0; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Ecommerce API Documentation</h1>\n" +
                "    <ul>\n" +
                "        <li><a href='/user-service/swagger-ui/index.html'>User Service API</a></li>\n" +
                "        <li><a href='/cart-service/swagger-ui/index.html'>Cart Service API</a></li>\n" +
                "        <!-- Add more services as needed -->\n" +
                "    </ul>\n" +
                "</body>\n" +
                "</html>";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}