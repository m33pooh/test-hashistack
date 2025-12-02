package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HelloController {

    private final RestTemplate rest = new RestTemplate();

    @Value("${VAULT_TOKEN:root}")
    private String vaultToken;

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> out = new HashMap<>();
        out.put("message", "Hello from Spring Boot (simple HashiStack example)");

        // Expose DB credentials (in real apps do not return secrets; this is for demo only)
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        out.put("db_user", dbUser != null ? dbUser : "(not set)");
        out.put("db_pass_present", dbPass != null);

        // Get services registered in Consul (may be empty)
        try {
            String consulUrl = "http://consul:8500/v1/agent/services";
            ResponseEntity<String> resp = rest.getForEntity(consulUrl, String.class);
            out.put("consul_services_raw", resp.getBody());
        } catch (Exception e) {
            out.put("consul_error", e.getMessage());
        }

        // Try to read a Vault KV (v2) at secret/data/myapp
        try {
            String vaultUrl = "http://vault:8200/v1/secret/data/myapp";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Vault-Token", vaultToken);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            ResponseEntity<String> resp = rest.exchange(vaultUrl, org.springframework.http.HttpMethod.GET, entity, String.class);
            out.put("vault_secret_raw", resp.getBody());
        } catch (Exception e) {
            out.put("vault_error", e.getMessage());
        }

        return out;
    }
}
