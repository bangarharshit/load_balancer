package com.example;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.servlet.http.HttpServletRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@SpringBootApplication
@RestController
public class LoadBalancerServer {
    private final Set<Integer> registeredPorts = new CopyOnWriteArraySet<>();
    private int currentServerIndex = 0;

    public static void main(String[] args) {
        SpringApplication.run(LoadBalancerServer.class, args);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerServer(@RequestBody ServerRegistration registration) {
        registeredPorts.add(registration.getPort());
        System.out.println("Registered server on port: " + registration.getPort());
        return ResponseEntity.ok("Server registered successfully");
    }

    @RequestMapping("/**")
    public ResponseEntity<String> proxyRequest(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {
        // How to get url from request. We also have to forward the request parameters
        String parameters = request.getQueryString();
        if (registeredPorts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("No servers available");
        }

        // Simple round-robin selection
        Integer[] ports = registeredPorts.toArray(new Integer[0]);
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