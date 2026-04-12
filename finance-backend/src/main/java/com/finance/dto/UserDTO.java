package com.finance.dto;

import lombok.*;

/**
 * UserDTO - Data Transfer Object for User.
 * We use DTOs to control what data we send/receive in API calls.
 * (Never expose raw entity directly - especially passwords!)
 */
public class UserDTO {

    // ---- Request DTO (what client sends when creating/updating user) ----
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String username;
        private String email;
        private String password;
        private String fullName;
        private String roleName; // e.g., "ADMIN", "MANAGER", "USER"
    }

    // ---- Update DTO (for updating user details) ----
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String email;
        private String fullName;
        private String roleName;
        private Boolean active;
    }

    // ---- Response DTO (what we return to client - NO password!) ----
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String roleName;
        private boolean active;
        private String createdAt;
    }

    // ---- Login Request ----
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
