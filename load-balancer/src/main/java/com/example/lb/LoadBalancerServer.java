package com.example.lb;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.servlet.http.HttpServletRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@RestController
@EnableScheduling  // Enable scheduling capabilities
public class LoadBalancerServer {
    private final Set<Integer> registeredPorts = new CopyOnWriteArraySet<>();    // All registered ports
    private final Set<Integer> activePorts = new CopyOnWriteArraySet<>();        // Currently healthy ports

    private int currentServerIndex = 0;
    private final RestTemplate restTemplate = new RestTemplate();

    public static void main(String[] args) {
        SpringApplication.run(LoadBalancerServer.class, args);
    }

    @Scheduled(fixedRate = 5000) // Run every 30 seconds
    public void healthCheck() {
        for (Integer port : registeredPorts) {
            String healthUrl = "http://localhost:" + port + "/health";

            try {
                ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    if (!activePorts.contains(port)) {
                        System.out.println("Server on port " + port + " is back online");
                        activePorts.add(port);
                    }
                } else {
                    System.out.println("Server on port " + port + " failed health check with status: " + response.getStatusCode());
                    activePorts.remove(port);
                }
            } catch (RestClientException e) {
                System.out.println("Server on port " + port + " is currently down: " + e.getMessage());
                activePorts.remove(port);
            }
        }
    }

    // Method to get current registered servers (useful for monitoring)
// Endpoint to get server status
    @GetMapping("/servers/status")
    public Map<String, Set<Integer>> getServersStatus() {
        return Map.of(
                "registered", registeredPorts,
                "active", activePorts
        );
    }


    @PostMapping("/register")
    public ResponseEntity<String> registerServer(@RequestBody ServerRegistration registration) {
        registeredPorts.add(registration.getPort());
        activePorts.add(registration.getPort());
        System.out.println("Registered server on port: " + registration.getPort());
        return ResponseEntity.ok("Server registered successfully");
    }

    @RequestMapping("/**")
    public ResponseEntity<String> proxyRequest(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {
        // How to get url from request. We also have to forward the request parameters
        String parameters = request.getQueryString();
        if (activePorts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("No servers available");
        }

        // Simple round-robin selection
        Integer[] ports = activePorts.toArray(new Integer[0]);
        int selectedPort = ports[currentServerIndex % ports.length];
        currentServerIndex++;

        String targetServer = "http://localhost:" + selectedPort;
        String forwardUrl = targetServer + request.getRequestURI()+"?"+parameters;
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames())
            .forEach(header -> headers.set(header, request.getHeader(header)));
        
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        
        return restTemplate.exchange(
            forwardUrl,
            HttpMethod.valueOf(request.getMethod()),
            entity,
            String.class
        );
    }

    @GetMapping("/")
    public String home() {
        return "Hello, World! This is a Spring Boot server.";
    }
}

class ServerRegistration {
    private int port;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
} 