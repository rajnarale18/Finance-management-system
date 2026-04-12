package com.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dto.UserDTO;
import com.finance.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserControllerTest - Integration-style tests for UserController HTTP layer.
 *
 * Uses @WebMvcTest to load only the web layer (no DB needed).
 * Tests: correct HTTP status codes, JSON responses, and role-based access.
 *
 * @WithMockUser simulates a logged-in user with a specific role.
 */
@WebMvcTest(UserController.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDTO.Response sampleResponse;
    private UserDTO.CreateRequest createRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = new UserDTO.Response(
                1L, "john", "john@example.com", "John Doe", "USER", true, "2024-04-01T10:00:00");

        createRequest = new UserDTO.CreateRequest(
                "john", "john@example.com", "pass123", "John Doe", "USER");
    }

    // ================================================================
    // POST /api/users - createUser
    // ================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/users: ADMIN can create user, returns 201 CREATED")
    void createUser_AsAdmin_Returns201() throws Exception {
        when(userService.createUser(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.roleName").value("USER"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/users: USER role is FORBIDDEN (403)")
    void createUser_AsUser_Returns403() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        verify(userService, never()).createUser(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("POST /api/users: MANAGER role is FORBIDDEN (403)")
    void createUser_AsManager_Returns403() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/users: returns 400 when service throws (e.g. duplicate username)")
    void createUser_DuplicateUsername_Returns400() throws Exception {
        when(userService.createUser(any()))
                .thenThrow(new RuntimeException("Username already exists: john"));

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Username already exists")));
    }

    // ================================================================
    // GET /api/users - getAllUsers
    // ================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/users: ADMIN gets list of all users, returns 200")
    void getAllUsers_AsAdmin_Returns200() throws Exception {
        UserDTO.Response user2 = new UserDTO.Response(
                2L, "jane", "jane@example.com", "Jane Smith", "MANAGER", true, "2024-04-02T10:00:00");

        when(userService.getAllUsers()).thenReturn(List.of(sampleResponse, user2));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("john"))
                .andExpect(jsonPath("$[1].username").value("jane"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/users: returns empty array when no users")
    void getAllUsers_Empty_Returns200WithEmptyArray() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/users: USER role is FORBIDDEN (403)")
    void getAllUsers_AsUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // GET /api/users/{id} - getUserById
    // ================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/users/{id}: returns 200 with correct user")
    void getUserById_Found_Returns200() throws Exception {
        when(userService.getUserById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/users/{id}: returns 404 when user not found")
    void getUserById_NotFound_Returns404() throws Exception {
        when(userService.getUserById(99L))
                .thenThrow(new RuntimeException("User not found with ID: 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound());
    }

    // ================================================================
    // PUT /api/users/{id} - updateUser
    // ================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/users/{id}: ADMIN can update user, returns 200")
    void updateUser_AsAdmin_Returns200() throws Exception {
        UserDTO.UpdateRequest updateReq = new UserDTO.UpdateRequest(
                "updated@example.com", "John Updated", null, null);
        UserDTO.Response updatedResponse = new UserDTO.Response(
                1L, "john", "updated@example.com", "John Updated", "USER", true, "2024-04-01T10:00:00");

        when(userService.updateUser(eq(1L), any())).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/users/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.fullName").value("John Updated"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/users/{id}: returns 400 when user not found")
    void updateUser_NotFound_Returns400() throws Exception {
        when(userService.updateUser(eq(99L), any()))
                .thenThrow(new RuntimeException("User not found with ID: 99"));

        mockMvc.perform(put("/api/users/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserDTO.UpdateRequest())))
                .andExpect(status().isBadRequest());
    }

    // ================================================================
    // DELETE /api/users/{id} - deleteUser
    // ================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/users/{id}: ADMIN can delete user, returns 200")
    void deleteUser_AsAdmin_Returns200() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("deactivated successfully")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/users/{id}: returns 404 when user not found")
    void deleteUser_NotFound_Returns404() throws Exception {
        doThrow(new RuntimeException("User not found with ID: 99"))
                .when(userService).deleteUser(99L);

        mockMvc.perform(delete("/api/users/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/users/{id}: USER role is FORBIDDEN (403)")
    void deleteUser_AsUser_Returns403() throws Exception {
        mockMvc.perform(delete("/api/users/1").with(csrf()))
                .andExpect(status().isForbidden());
        verify(userService, never()).deleteUser(any());
    }
}
