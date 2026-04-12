package com.finance.controller;

import com.finance.dto.SummaryDTO;
import com.finance.service.SummaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SummaryControllerTest - HTTP tests for SummaryController.
 *
 * Tests: overall summary access, per-user summary access, role restrictions.
 */
@WebMvcTest(SummaryController.class)
@DisplayName("SummaryController Tests")
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SummaryService summaryService;

    // ================================================================
    // GET /api/summary/overall
    // ================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/summary/overall: ADMIN sees full financial summary, returns 200")
    void getOverallSummary_AsAdmin_Returns200() throws Exception {
        SummaryDTO summary = new SummaryDTO(
                new BigDecimal("95000"), new BigDecimal("20000"),
                new BigDecimal("75000"), 2L, 3L, 1L,
                Map.of("Salary", new BigDecimal("95000"), "Rent", new BigDecimal("12000")));

        when(summaryService.getOverallSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/summary/overall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(95000))
                .andExpect(jsonPath("$.totalExpense").value(20000))
                .andExpect(jsonPath("$.netBalance").value(75000))
                .andExpect(jsonPath("$.pendingTransactions").value(2))
                .andExpect(jsonPath("$.approvedTransactions").value(3))
                .andExpect(jsonPath("$.rejectedTransactions").value(1))
                .andExpect(jsonPath("$.categoryBreakdown.Salary").value(95000));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /api/summary/overall: MANAGER can also see overall summary")
    void getOverallSummary_AsManager_Returns200() throws Exception {
        SummaryDTO summary = new SummaryDTO(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0L, 0L, 0L, Map.of());

        when(summaryService.getOverallSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/summary/overall"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/summary/overall: USER role is FORBIDDEN (403)")
    void getOverallSummary_AsUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/summary/overall"))
                .andExpect(status().isForbidden());

        verify(summaryService, never()).getOverallSummary();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/summary/overall: returns zero values when no approved transactions")
    void getOverallSummary_EmptyDB_ReturnsZeros() throws Exception {
        SummaryDTO empty = new SummaryDTO(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0L, 0L, 0L, Map.of());

        when(summaryService.getOverallSummary()).thenReturn(empty);

        mockMvc.perform(get("/api/summary/overall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.netBalance").value(0));
    }

    // ================================================================
    // GET /api/summary/user/{userId}
    // ================================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/summary/user/{id}: USER can see own summary, returns 200")
    void getUserSummary_AsUser_Returns200() throws Exception {
        SummaryDTO userSummary = new SummaryDTO(
                new BigDecimal("50000"), new BigDecimal("12000"),
                new BigDecimal("38000"), 1L, 1L, 0L, null);

        when(summaryService.getUserSummary(1L)).thenReturn(userSummary);

        mockMvc.perform(get("/api/summary/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(50000))
                .andExpect(jsonPath("$.totalExpense").value(12000))
                .andExpect(jsonPath("$.netBalance").value(38000))
                .andExpect(jsonPath("$.pendingTransactions").value(1))
                .andExpect(jsonPath("$.approvedTransactions").value(1))
                .andExpect(jsonPath("$.rejectedTransactions").value(0));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /api/summary/user/{id}: MANAGER can view any user's summary")
    void getUserSummary_AsManager_Returns200() throws Exception {
        SummaryDTO summary = new SummaryDTO(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0L, 0L, 0L, null);

        when(summaryService.getUserSummary(2L)).thenReturn(summary);

        mockMvc.perform(get("/api/summary/user/2"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/summary/user/{id}: ADMIN can view any user's summary")
    void getUserSummary_AsAdmin_Returns200() throws Exception {
        SummaryDTO summary = new SummaryDTO(
                new BigDecimal("10000"), new BigDecimal("5000"),
                new BigDecimal("5000"), 0L, 2L, 0L, null);

        when(summaryService.getUserSummary(3L)).thenReturn(summary);

        mockMvc.perform(get("/api/summary/user/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netBalance").value(5000));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/summary/user/{id}: negative netBalance is returned correctly")
    void getUserSummary_NegativeBalance_ReturnedCorrectly() throws Exception {
        SummaryDTO summary = new SummaryDTO(
                new BigDecimal("5000"), new BigDecimal("20000"),
                new BigDecimal("-15000"), 0L, 2L, 0L, null);

        when(summaryService.getUserSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/summary/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netBalance").value(-15000));
    }
}
