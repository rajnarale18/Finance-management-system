package com.finance.controller;

import com.finance.dto.SummaryDTO;
import com.finance.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * SummaryController - Handles financial summary/report endpoints.
 *
 * Base URL: /api/summary
 *
 * Endpoints:
 * GET /api/summary/overall        - Overall summary    (ADMIN, MANAGER)
 * GET /api/summary/user/{userId}  - Per-user summary   (All roles)
 */
@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    @Autowired
    private SummaryService summaryService;

    // ---- Overall Summary (all users combined) ----
    @GetMapping("/overall")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SummaryDTO> getOverallSummary() {
        return ResponseEntity.ok(summaryService.getOverallSummary());
    }

    // ---- Per-User Summary ----
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<SummaryDTO> getUserSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(summaryService.getUserSummary(userId));
    }
}
