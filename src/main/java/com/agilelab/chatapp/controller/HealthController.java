package com.agilelab.chatapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping({"/api/health", "/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "AgileLab-Backend",
                "timestamp", Instant.now().toString(),
                "framework", "Spring Boot 3.4.0"
        ));
    }
}

