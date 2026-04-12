package com.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dto.TransactionDTO;
import com.finance.model.Transaction.TransactionStatus;
import com.finance.model.Transaction.TransactionType;
import com.finance.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TransactionControllerTest - HTTP layer tests for TransactionController.
 *
 * Covers: status codes, JSON response fields, role-based access restrictions,
 * filter endpoints, and approve/reject workflow.
 */
@WebMvcTest(TransactionController.class)
@DisplayName("TransactionController Tests")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    private TransactionDTO.Response sampleResponse;
    private TransactionDTO.CreateRequest createRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = new TransactionDTO.Response(
                1L, "INCOME", new BigDecimal("50000.00"), "INR",
                "Salary", "April salary", "2024-04-01", "PENDING", "john", "2024-04-01T10:00:00");

        createRequest = new TransactionDTO.CreateRequest(
                TransactionType.INCOME, new BigDecimal("50000"),
                "INR", "Salary", "April salary", "2024-04-01");
    }

    // ================================================================
    // POST /api/transactions/user/{userId} - createTransaction
    // ================================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/transactions/user/{id}: USER can create, returns 201")
    void createTransaction_AsUser_Returns201() throws Exception {
        when(transactionService.createTransaction(any(), eq(1L))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/transactions/user/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.createdByUsername").value("john"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("POST /api/transactions/user/{id}: MANAGER can also create, returns 201")
    void createTransaction_AsManager_Returns201() throws Exception {
        when(transactionService.createTransaction(any(), eq(1L))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/transactions/user/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/transactions/user/{id}: returns 400 when user not found")
    void createTransaction_UserNotFound_Returns400() throws Exception {
        when(transactionService.createTransaction(any(), eq(99L)))
                .thenThrow(new RuntimeException("User not found with ID: 99"));

        mockMvc.perform(post("/api/transactions/user/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("User not found")));
    }

    // ================================================================
    // GET /api/transactions - getAllTransactions
    // ================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/transactions: ADMIN gets all transactions, returns 200")
    void getAllTransactions_AsAdmin_Returns200() throws Exception {
        TransactionDTO.Response t2 = new TransactionDTO.Response(
                2L, "EXPENSE", new BigDecimal("5000"), "INR",
                "Food", "Groceries", "2024-04-05", "APPROVED", "jane", "2024-04-05T09:00:00");

        when(transactionService.getAllTransactions()).thenReturn(List.of(sampleResponse, t2));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("INCOME"))
                .andExpect(jsonPath("$[1].type").value("EXPENSE"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /api/transactions: MANAGER can view all transactions")
    void getAllTransactions_AsManager_Returns200() throws Exception {
        when(transactionService.getAllTransactions()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/transactions: USER role is FORBIDDEN (403)")
    void getAllTransactions_AsUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isForbidden());

        verify(transactionService, never()).getAllTransactions();
    }

    // ================================================================
    // GET /api/transactions/{id} - getTransactionById
    // ================================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/transactions/{id}: returns 200 with correct transaction")
    void getTransactionById_Found_Returns200() throws Exception {
        when(transactionService.getTransactionById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.category").value("Salary"))
                .andExpect(jsonPath("$.transactionDate").value("2024-04-01"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/transactions/{id}: returns 404 when not found")
    void getTransactionById_NotFound_Returns404() throws Exception {
        when(transactionService.getTransactionById(99L))
                .thenThrow(new RuntimeException("Transaction not found with ID: 99"));

        mockMvc.perform(get("/api/transactions/99"))
                .andExpect(status().isNotFound());
    }

    // ================================================================
    // GET /api/transactions/user/{userId} - getByUser
    // ================================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/transactions/user/{id}: returns this user's transactions")
    void getByUser_Returns200() throws Exception {
        when(transactionService.getTransactionsByUser(1L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/transactions/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].createdByUsername").value("john"));
    }

    // ================================================================
    // GET /api/transactions/filter/type - filterByType
    // ================================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/transactions/filter/type?type=INCOME: filters by INCOME")
    void filterByType_Income_Returns200() throws Exception {
        when(transactionService.getByType(TransactionType.INCOME))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/transactions/filter/type")
                        .param("type", "INCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("INCOME"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/transactions/filter/type?type=EXPENSE: filters by EXPENSE")
    void filterByType_Expense_Returns200() throws Exception {
        TransactionDTO.Response expense = new TransactionDTO.Response(
                2L, "EXPENSE", new BigDecimal("5000"), "INR",
                "Food", "Grocery", "2024-04-05", "PENDING", "john", "2024-04-05T00:00:00");

        when(transactionService.getByType(TransactionType.EXPENSE))
                .thenReturn(List.of(expense));

        mockMvc.perform(get("/api/transactions/filter/type")
                        .param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("EXPENSE"));
    }

    // ================================================================
    // GET /api/transactions/filter/status - filterByStatus
    // ================================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /api/transactions/filter/status?status=PENDING: MANAGER can filter by status")
    void filterByStatus_Pending_Returns200() throws Exception {
        when(transactionService.getByStatus(TransactionStatus.PENDING))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/transactions/filter/status")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/transactions/filter/status: USER is FORBIDDEN (403)")
    void filterByStatus_AsUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/transactions/filter/status")
                        .param("status", "PENDING"))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // GET /api/transactions/filter/date - filterByDateRange
    // ================================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/transactions/filter/date: filters by date range correctly")
    void filterByDateRange_Returns200() throws Exception {
        when(transactionService.getByDateRange("2024-04-01", "2024-04-30"))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/transactions/filter/date")
                        .param("start", "2024-04-01")
                        .param("end", "2024-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/transactions/filter/date: returns empty when no results in range")
    void filterByDateRange_NoResults_ReturnsEmptyList() throws Exception {
        when(transactionService.getByDateRange(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions/filter/date")
                        .param("start", "2023-01-01")
                        .param("end", "2023-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ================================================================
    // PATCH /api/transactions/{id}/status - updateStatus (Approve/Reject)
    // ================================================================

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("PATCH /api/transactions/{id}/status: MANAGER can APPROVE, returns 200")
    void updateStatus_Approve_AsManager_Returns200() throws Exception {
        TransactionDTO.Response approved = new TransactionDTO.Response(
                1L, "INCOME", new BigDecimal("50000"), "INR",
                "Salary", "April salary", "2024-04-01", "APPROVED", "john", "2024-04-01T10:00:00");

        TransactionDTO.StatusUpdateRequest statusReq = new TransactionDTO.StatusUpdateRequest(
                TransactionStatus.APPROVED);

        when(transactionService.updateStatus(eq(1L), eq(TransactionStatus.APPROVED)))
                .thenReturn(approved);

        mockMvc.perform(patch("/api/transactions/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/transactions/{id}/status: ADMIN can REJECT, returns 200")
    void updateStatus_Reject_AsAdmin_Returns200() throws Exception {
        TransactionDTO.Response rejected = new TransactionDTO.Response(
                1L, "INCOME", new BigDecimal("50000"), "INR",
                "Salary", "April salary", "2024-04-01", "REJECTED", "john", "2024-04-01T10:00:00");

        TransactionDTO.StatusUpdateRequest statusReq = new TransactionDTO.StatusUpdateRequest(
                TransactionStatus.REJECTED);

        when(transactionService.updateStatus(eq(1L), eq(TransactionStatus.REJECTED)))
                .thenReturn(rejected);

        mockMvc.perform(patch("/api/transactions/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("PATCH /api/transactions/{id}/status: USER cannot approve — FORBIDDEN (403)")
    void updateStatus_AsUser_Returns403() throws Exception {
        TransactionDTO.StatusUpdateRequest req = new TransactionDTO.StatusUpdateRequest(
                TransactionStatus.APPROVED);

        mockMvc.perform(patch("/api/transactions/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        verify(transactionService, never()).updateStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("PATCH /api/transactions/{id}/status: returns 400 when transaction not found")
    void updateStatus_NotFound_Returns400() throws Exception {
        TransactionDTO.StatusUpdateRequest req = new TransactionDTO.StatusUpdateRequest(
                TransactionStatus.APPROVED);

        when(transactionService.updateStatus(eq(99L), any()))
                .thenThrow(new RuntimeException("Transaction not found with ID: 99"));

        mockMvc.perform(patch("/api/transactions/99/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ================================================================
    // DELETE /api/transactions/{id} - deleteTransaction
    // ================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/transactions/{id}: ADMIN can delete, returns 200")
    void deleteTransaction_AsAdmin_Returns200() throws Exception {
        doNothing().when(transactionService).deleteTransaction(1L);

        mockMvc.perform(delete("/api/transactions/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("deleted successfully")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/transactions/{id}: USER role is FORBIDDEN (403)")
    void deleteTransaction_AsUser_Returns403() throws Exception {
        mockMvc.perform(delete("/api/transactions/1").with(csrf()))
                .andExpect(status().isForbidden());
        verify(transactionService, never()).deleteTransaction(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("DELETE /api/transactions/{id}: MANAGER role is FORBIDDEN (403)")
    void deleteTransaction_AsManager_Returns403() throws Exception {
        mockMvc.perform(delete("/api/transactions/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/transactions/{id}: returns 404 when transaction not found")
    void deleteTransaction_NotFound_Returns404() throws Exception {
        doThrow(new RuntimeException("Transaction not found with ID: 99"))
                .when(transactionService).deleteTransaction(99L);

        mockMvc.perform(delete("/api/transactions/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
