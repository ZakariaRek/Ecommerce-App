package com.Ecommerce.Cart.Service.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Test", description = "Test endpoints")
public class TestController {

    @Operation(summary = "Test endpoint", description = "Simple test endpoint to verify Swagger is working")
    @GetMapping
    public String test() {
        return "Swagger test successful";
    }
}