package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class HelloController {

    private final RestTemplate rest = new RestTemplate();

    @Autowired
    private NullSafetyExample nullSafetyExample;

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

    /**
     * Endpoint demonstrating null safety patterns
     */
    @GetMapping("/null-safety-demo")
    public Map<String, Object> nullSafetyDemo(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email) {
        
        Map<String, Object> result = new HashMap<>();
        
        // Example 1: Display name with null safety
        String displayName = nullSafetyExample.getDisplayName(firstName, lastName);
        result.put("display_name", displayName);
        
        // Example 2: Optional chaining
        Optional<String> safeFind = nullSafetyExample.safeFind(email);
        result.put("email_found", safeFind.isPresent());
        result.put("email_value", safeFind.orElse("Not found"));
        
        // Example 3: String processing with null handling
        int emailLength = nullSafetyExample.getLength(email);
        result.put("email_length", emailLength);
        
        // Example 4: Null-safe UserInfo record
        NullSafetyExample.UserInfo userInfo = new NullSafetyExample.UserInfo("user-" + System.nanoTime(), 
            displayName != null ? displayName : "Unknown User", 
            email, 
            null);
        result.put("user_info", Map.of(
            "id", userInfo.id(),
            "name", userInfo.name(),
            "contact", userInfo.getContactInfo()
        ));
        
        result.put("message", "Null safety example - Spring Boot 4.0.0 with Java 25");
        return result;
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/version")
    public Map<String, String> version() {
        Map<String, String> info = new HashMap<>();
        info.put("spring_boot_version", "4.0.0");
        info.put("java_version", "25");
        info.put("message", "Running with enhanced null safety features");
        return info;
    }
}
