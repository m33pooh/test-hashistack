package com.example.demo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Demonstrates null safety patterns in Java 25 with Spring Boot 4.0.0
 * 
 * This class showcases best practices for handling null values using:
 * 1. NotNull and Nullable annotations for IDE support
 * 2. Optional for functional-style null handling
 * 3. Objects.requireNonNull() for explicit null checks
 */
@Service
@Slf4j
public class NullSafetyExample {

    /**
     * Example 1: Using @NotNull annotation
     * IDE will warn if null is passed to this method
     */
    public String processString(@NotNull String input) {
        // Since input is annotated as @NotNull, the IDE knows it won't be null
        return input.toUpperCase();
    }

    /**
     * Example 2: Using @Nullable annotation
     * IDE will warn if the return value is used without null checking
     */
    @Nullable
    public String findOptionalValue(String key) {
        if ("valid".equals(key)) {
            return "found: " + key;
        }
        return null; // Explicitly indicating this can return null
    }

    /**
     * Example 3: Using Optional for better null handling
     * Returns Optional instead of null, forcing callers to handle the absence of
     * value
     */
    public Optional<String> safeFind(String key) {
        if ("valid".equals(key)) {
            return Optional.of("found: " + key);
        }
        return Optional.empty();
    }

    /**
     * Example 4: Objects.requireNonNull() for explicit null validation
     * Throws NullPointerException with a custom message if value is null
     */
    public void validateRequired(@NotNull String userId, @NotNull String email) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(email, "email cannot be null");

        // Safe to use userId and email now
        processUserData(userId, email);
    }

    /**
     * Example 5: Defensive null checking pattern
     */
    public int getLength(@Nullable String value) {
        if (value == null) {
            return 0;
        }
        return value.length();
    }

    /**
     * Example 6: Optional with functional operations
     * Using Optional with map, filter, and orElse for elegant null handling
     */
    public String getDisplayName(@Nullable String firstName, @Nullable String lastName) {
        String first = Optional.ofNullable(firstName)
                .filter(f -> !f.isBlank())
                .orElse("Unknown");

        String last = Optional.ofNullable(lastName)
                .filter(l -> !l.isBlank())
                .orElse("User");

        return first + " " + last;
    }

    /**
     * Example 7: Chaining Optional operations
     * Demonstrates how to work with potentially null nested objects
     */
    public Optional<String> getUserEmail(@Nullable User user) {
        return Optional.ofNullable(user)
                .map(User::getProfile)
                .map(Profile::getEmail)
                .filter(email -> email.contains("@"));
    }

    /**
     * Example 8: Using Optional.ifPresentOrElse (Java 9+)
     */
    public String processValueOrDefault(@Nullable String value, String defaultValue) {
        Optional<String> opt = Optional.ofNullable(value)
                .filter(v -> !v.isBlank());

        opt.ifPresentOrElse(
                v -> log.info("Processing: {}", v),
                () -> log.info("Using default: {}", defaultValue));

        return opt.orElse(defaultValue);
    }

    /**
     * Example 9: Null-safe method chaining with Optional
     */
    public Optional<Integer> getUserAgeIfAdult(@Nullable User user) {
        return Optional.ofNullable(user)
                .map(User::getAge)
                .filter(age -> age >= 18);
    }

    /**
     * Example 10: Using record with nullable fields (Java 16+)
     */
    public record UserInfo(
            @NotNull String id,
            @NotNull String name,
            @Nullable String email,
            @Nullable String phone) {
        // Constructor validation
        public UserInfo {
            Objects.requireNonNull(id, "id cannot be null");
            Objects.requireNonNull(name, "name cannot be null");
        }

        public String getContactInfo() {
            return email != null ? email : (phone != null ? phone : "No contact info");
        }
    }

    // Helper classes for demonstration
    public static class User {
        private String name;
        private int age;
        private Profile profile;

        public User(String name, int age, Profile profile) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            this.age = age;
            this.profile = profile;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public Profile getProfile() {
            return profile;
        }
    }

    public static class Profile {
        private String email;

        public Profile(String email) {
            this.email = email;
        }

        public String getEmail() {
            return email;
        }
    }

    /**
     * Example 11: Demonstrating null safety in streams
     */
    public Optional<String> findFirstValidEmail(@Nullable java.util.List<String> emails) {
        return Optional.ofNullable(emails)
                .stream()
                .flatMap(java.util.Collection::stream)
                .filter(email -> email.contains("@"))
                .findFirst();
    }

    /**
     * Example 12: Safe field access
     */
    public String getUserDisplayName(@Nullable User user) {
        return Optional.ofNullable(user)
                .map(User::getName)
                .orElse("Anonymous");
    }

    private void processUserData(String userId, String email) {
        // Process user data safely
        log.info("Processing user: {} with email: {}", userId, email);
    }
}
