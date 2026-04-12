package com.finance.service;

import com.finance.dto.UserDTO;
import com.finance.model.Role;
import com.finance.model.User;
import com.finance.repository.RoleRepository;
import com.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserServiceTest - Unit tests for UserService.
 *
 * Uses Mockito to mock repositories — no database needed.
 * Tests every public method: createUser, getAllUsers, getUserById, updateUser, deleteUser.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private Role userRole;
    private Role adminRole;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        userRole  = new Role(1L, "USER",  "Standard user");
        adminRole = new Role(2L, "ADMIN", "Administrator");

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("john");
        sampleUser.setEmail("john@example.com");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setFullName("John Doe");
        sampleUser.setRole(userRole);
        sampleUser.setActive(true);
        sampleUser.setCreatedAt(LocalDateTime.now());
    }

    // ================================================================
    // createUser
    // ================================================================

    @Test
    @DisplayName("createUser: should return saved user DTO on success")
    void createUser_Success() {
        UserDTO.CreateRequest req = new UserDTO.CreateRequest(
                "john", "john@example.com", "pass123", "John Doe", "USER");

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserDTO.Response result = userService.createUser(req);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("john");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getRoleName()).isEqualTo("USER");
        assertThat(result.isActive()).isTrue();

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("pass123");
    }

    @Test
    @DisplayName("createUser: should throw when username already taken")
    void createUser_DuplicateUsername_Throws() {
        UserDTO.CreateRequest req = new UserDTO.CreateRequest(
                "john", "john@example.com", "pass123", "John Doe", "USER");

        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser: should throw when email already registered")
    void createUser_DuplicateEmail_Throws() {
        UserDTO.CreateRequest req = new UserDTO.CreateRequest(
                "newuser", "john@example.com", "pass123", "New", "USER");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser: should throw when role does not exist")
    void createUser_RoleNotFound_Throws() {
        UserDTO.CreateRequest req = new UserDTO.CreateRequest(
                "john", "john@example.com", "pass123", "John Doe", "GHOST");

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Role not found");
    }

    @Test
    @DisplayName("createUser: password must be encoded, never stored in plain text")
    void createUser_PasswordIsEncoded() {
        UserDTO.CreateRequest req = new UserDTO.CreateRequest(
                "john", "john@example.com", "myPlainPassword", "John Doe", "USER");

        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("myPlainPassword")).thenReturn("$2a$bcryptHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            // Assert password was encoded before save
            assertThat(u.getPassword()).isEqualTo("$2a$bcryptHash");
            assertThat(u.getPassword()).doesNotContain("myPlainPassword");
            return sampleUser;
        });

        userService.createUser(req);
        verify(passwordEncoder).encode("myPlainPassword");
    }

    // ================================================================
    // getAllUsers
    // ================================================================

    @Test
    @DisplayName("getAllUsers: should return all users as response DTOs")
    void getAllUsers_ReturnsList() {
        User user2 = new User();
        user2.setId(2L); user2.setUsername("jane"); user2.setEmail("jane@example.com");
        user2.setFullName("Jane Smith"); user2.setRole(adminRole); user2.setActive(true);

        when(userRepository.findAll()).thenReturn(List.of(sampleUser, user2));

        List<UserDTO.Response> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserDTO.Response::getUsername)
                          .containsExactly("john", "jane");
    }

    @Test
    @DisplayName("getAllUsers: should return empty list when no users")
    void getAllUsers_Empty() {
        when(userRepository.findAll()).thenReturn(List.of());
        assertThat(userService.getAllUsers()).isEmpty();
    }

    // ================================================================
    // getUserById
    // ================================================================

    @Test
    @DisplayName("getUserById: should return correct user when found")
    void getUserById_Found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserDTO.Response result = userService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("john");
        assertThat(result.getRoleName()).isEqualTo("USER");
    }

    @Test
    @DisplayName("getUserById: should throw when user does not exist")
    void getUserById_NotFound_Throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with ID: 99");
    }

    // ================================================================
    // updateUser
    // ================================================================

    @Test
    @DisplayName("updateUser: should update email and fullName")
    void updateUser_UpdatesEmailAndName() {
        UserDTO.UpdateRequest req = new UserDTO.UpdateRequest(
                "new@example.com", "John Updated", null, null);

        User updated = new User();
        updated.setId(1L); updated.setUsername("john");
        updated.setEmail("new@example.com"); updated.setFullName("John Updated");
        updated.setRole(userRole); updated.setActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenReturn(updated);

        UserDTO.Response result = userService.updateUser(1L, req);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getFullName()).isEqualTo("John Updated");
    }

    @Test
    @DisplayName("updateUser: should update role when roleName provided")
    void updateUser_UpdatesRole() {
        UserDTO.UpdateRequest req = new UserDTO.UpdateRequest(null, null, "ADMIN", null);

        User updated = new User();
        updated.setId(1L); updated.setUsername("john");
        updated.setEmail("john@example.com"); updated.setFullName("John Doe");
        updated.setRole(adminRole); updated.setActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any())).thenReturn(updated);

        UserDTO.Response result = userService.updateUser(1L, req);
        assertThat(result.getRoleName()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("updateUser: should deactivate user when active=false")
    void updateUser_DeactivatesUser() {
        UserDTO.UpdateRequest req = new UserDTO.UpdateRequest(null, null, null, false);

        User deactivated = new User();
        deactivated.setId(1L); deactivated.setUsername("john");
        deactivated.setEmail("john@example.com"); deactivated.setFullName("John Doe");
        deactivated.setRole(userRole); deactivated.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenReturn(deactivated);

        UserDTO.Response result = userService.updateUser(1L, req);
        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("updateUser: should throw when user not found")
    void updateUser_NotFound_Throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updateUser(99L, new UserDTO.UpdateRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with ID: 99");
    }

    @Test
    @DisplayName("updateUser: should throw when new role does not exist")
    void updateUser_InvalidRole_Throws() {
        UserDTO.UpdateRequest req = new UserDTO.UpdateRequest(null, null, "SUPERUSER", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(roleRepository.findByName("SUPERUSER")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updateUser(1L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Role not found");
    }

    // ================================================================
    // deleteUser
    // ================================================================

    @Test
    @DisplayName("deleteUser: should soft-delete (deactivate) user, not remove from DB")
    void deleteUser_SoftDeletes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenReturn(sampleUser);

        userService.deleteUser(1L);

        // Verify user was saved with active=false
        verify(userRepository).save(argThat(u -> !u.isActive()));
        // Verify deleteById was NEVER called (it's a soft delete)
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteUser: should throw when user not found")
    void deleteUser_NotFound_Throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with ID: 99");
        verify(userRepository, never()).save(any());
    }
}
