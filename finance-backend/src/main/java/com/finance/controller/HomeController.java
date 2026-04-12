package com.finance.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HomeController - Handles the root URL: http://localhost:8080/
 *
 * Without this, visiting the root URL shows a "Whitelabel Error Page" (404).
 * This controller returns a simple JSON welcome message with all available API endpoints.
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("app", "Finance Data Processing & Access Control Backend");
        response.put("status", "✅ Running");
        response.put("version", "1.0.0");

        // ---- Available Endpoints ----
        Map<String, String> endpoints = new LinkedHashMap<>();

        // User endpoints (ADMIN only)
        endpoints.put("POST   /api/users",              "Create a new user (ADMIN)");
        endpoints.put("GET    /api/users",              "Get all users (ADMIN)");
        endpoints.put("GET    /api/users/{id}",         "Get user by ID (ADMIN)");
        endpoints.put("PUT    /api/users/{id}",         "Update user (ADMIN)");
        endpoints.put("DELETE /api/users/{id}",         "Deactivate user (ADMIN)");

        // Transaction endpoints
        endpoints.put("POST   /api/transactions/user/{userId}", "Create transaction (ALL)");
        endpoints.put("GET    /api/transactions",               "Get all transactions (MANAGER, ADMIN)");
        endpoints.put("GET    /api/transactions/{id}",          "Get transaction by ID (ALL)");
        endpoints.put("GET    /api/transactions/user/{userId}", "Get by user (ALL)");
        endpoints.put("GET    /api/transactions/filter/type",   "Filter by type: ?type=INCOME (ALL)");
        endpoints.put("GET    /api/transactions/filter/status", "Filter by status: ?status=PENDING (MANAGER, ADMIN)");
        endpoints.put("GET    /api/transactions/filter/date",   "Filter by date: ?start=2024-04-01&end=2024-04-30 (ALL)");
        endpoints.put("PATCH  /api/transactions/{id}/status",   "Approve/Reject (MANAGER, ADMIN)");
        endpoints.put("DELETE /api/transactions/{id}",          "Delete transaction (ADMIN)");

        // Summary endpoints
        endpoints.put("GET    /api/summary/overall",      "Overall financial summary (MANAGER, ADMIN)");
        endpoints.put("GET    /api/summary/user/{userId}", "Per-user summary (ALL)");

        response.put("endpoints", endpoints);

        // ---- Default Credentials ----
        Map<String, String> credentials = new LinkedHashMap<>();
        credentials.put("ADMIN",   "username: admin   | password: admin123");
        credentials.put("MANAGER", "username: manager | password: manager123");
        credentials.put("USER",    "username: user1   | password: user123");

        response.put("defaultCredentials", credentials);
        response.put("note", "Use Postman with Basic Auth to test the endpoints above.");

        return response;
    }
}
