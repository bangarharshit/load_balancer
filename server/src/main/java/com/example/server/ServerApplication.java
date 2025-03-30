package com.example.server;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@SpringBootApplication
@RestController
public class ServerApplication {

    @Value("${server.port}")
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerServer() {
        try {
            String registrationUrl = "http://localhost:8080/register";
            Map<String, Integer> data = Map.of("port", port);
            ResponseEntity<String> response = restTemplate.postForEntity(registrationUrl, data, String.class);
            System.out.println("Registration response: " + response.getBody());
        } catch (Exception e) {
            System.out.println("Failed to register server: " + e.getMessage());
        }
    }

    @GetMapping("/")
    public String home() {
        return String.format("Server on port %d is running!", port);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy"));
    }

    @GetMapping("/api")
    public String apiGet(@RequestParam(defaultValue = "No message provided") String message) {
        System.out.printf("Server on port %d received: %s%n", port, message);
        return String.format("Server on port %d received: %s", port, message);
    }

    @PostMapping("/api")
    public String apiPost(@RequestBody Object data) {
        System.out.printf("Server on port %d received: %s%n", port, data);
        return String.format("Server on port %d received: %s", port, data);
    }
}