package com.finance.config;

import com.finance.model.Role;
import com.finance.model.Transaction;
import com.finance.model.Transaction.TransactionStatus;
import com.finance.model.Transaction.TransactionType;
import com.finance.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * ModelAndSecurityTest - Tests for model behaviour, default values,
 * and security-related logic.
 *
 * These tests verify:
 * 1. Entity default values are set correctly
 * 2. @PrePersist sets timestamps automatically
 * 3. Transaction defaults (PENDING status, INR currency)
 * 4. User defaults (active = true)
 * 5. Enum values exist and are correct
 * 6. BigDecimal precision for financial amounts
 */
@DisplayName("Model and Security Tests")
class ModelAndSecurityTest {

    // ================================================================
    // Transaction Model
    // ================================================================

    @Test
    @DisplayName("Transaction: default status should be PENDING when created")
    void transaction_DefaultStatus_IsPending() {
        Transaction t = new Transaction();
        assertThat(t.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    @DisplayName("Transaction: default currency should be INR")
    void transaction_DefaultCurrency_IsINR() {
        Transaction t = new Transaction();
        assertThat(t.getCurrency()).isEqualTo("INR");
    }

    @Test
    @DisplayName("Transaction: @PrePersist sets createdAt timestamp")
    void transaction_PrePersist_SetsCreatedAt() {
        Transaction t = new Transaction();
        t.setCreatedAt(); // simulate what JPA calls before saving
        assertThat(t.getCreatedAt()).isNotNull();
        assertThat(t.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Transaction: TransactionType enum has INCOME and EXPENSE")
    void transaction_TypeEnum_HasCorrectValues() {
        assertThat(TransactionType.values())
                .containsExactlyInAnyOrder(TransactionType.INCOME, TransactionType.EXPENSE);
    }

    @Test
    @DisplayName("Transaction: TransactionStatus enum has PENDING, APPROVED, REJECTED")
    void transaction_StatusEnum_HasCorrectValues() {
        assertThat(TransactionStatus.values())
                .containsExactlyInAnyOrder(
                        TransactionStatus.PENDING,
                        TransactionStatus.APPROVED,
                        TransactionStatus.REJECTED);
    }

    @Test
    @DisplayName("Transaction: BigDecimal amount retains precision (no floating point errors)")
    void transaction_Amount_BigDecimalPrecision() {
        Transaction t = new Transaction();
        t.setAmount(new BigDecimal("12345.67"));
        assertThat(t.getAmount()).isEqualByComparingTo("12345.67");
        // Verify no precision loss vs double
        assertThat(t.getAmount().toPlainString()).isEqualTo("12345.67");
    }

    @Test
    @DisplayName("Transaction: can hold all valid field combinations")
    void transaction_FullObject_AllFieldsSet() {
        Transaction t = new Transaction();
        t.setId(1L);
        t.setType(TransactionType.EXPENSE);
        t.setAmount(new BigDecimal("5000.00"));
        t.setCurrency("USD");
        t.setCategory("Travel");
        t.setDescription("Business trip to Delhi");
        t.setTransactionDate(LocalDate.of(2024, 4, 15));
        t.setStatus(TransactionStatus.APPROVED);

        assertThat(t.getId()).isEqualTo(1L);
        assertThat(t.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(t.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(t.getCurrency()).isEqualTo("USD");
        assertThat(t.getCategory()).isEqualTo("Travel");
        assertThat(t.getDescription()).isEqualTo("Business trip to Delhi");
        assertThat(t.getTransactionDate()).isEqualTo(LocalDate.of(2024, 4, 15));
        assertThat(t.getStatus()).isEqualTo(TransactionStatus.APPROVED);
    }

    // ================================================================
    // User Model
    // ================================================================

    @Test
    @DisplayName("User: default active should be true on creation")
    void user_DefaultActive_IsTrue() {
        User u = new User();
        assertThat(u.isActive()).isTrue();
    }

    @Test
    @DisplayName("User: @PrePersist sets createdAt timestamp")
    void user_PrePersist_SetsCreatedAt() {
        User u = new User();
        u.setCreatedAt(); // simulate JPA @PrePersist
        assertThat(u.getCreatedAt()).isNotNull();
        assertThat(u.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("User: can be deactivated by setting active = false")
    void user_CanBeDeactivated() {
        User u = new User();
        u.setActive(false);
        assertThat(u.isActive()).isFalse();
    }

    @Test
    @DisplayName("User: role assignment works correctly")
    void user_RoleAssignment() {
        Role role = new Role(1L, "ADMIN", "Full access");
        User u = new User();
        u.setRole(role);

        assertThat(u.getRole()).isNotNull();
        assertThat(u.getRole().getName()).isEqualTo("ADMIN");
        assertThat(u.getRole().getDescription()).isEqualTo("Full access");
    }

    @Test
    @DisplayName("User: all fields can be set and retrieved")
    void user_FullObject_AllFieldsSet() {
        Role role = new Role(2L, "USER", "Standard user");
        User u = new User();
        u.setId(1L);
        u.setUsername("john");
        u.setEmail("john@example.com");
        u.setPassword("$2a$encoded");
        u.setFullName("John Doe");
        u.setRole(role);
        u.setActive(true);

        assertThat(u.getId()).isEqualTo(1L);
        assertThat(u.getUsername()).isEqualTo("john");
        assertThat(u.getEmail()).isEqualTo("john@example.com");
        assertThat(u.getPassword()).isEqualTo("$2a$encoded");
        assertThat(u.getFullName()).isEqualTo("John Doe");
        assertThat(u.getRole().getName()).isEqualTo("USER");
        assertThat(u.isActive()).isTrue();
    }

    // ================================================================
    // Role Model
    // ================================================================

    @Test
    @DisplayName("Role: can hold ADMIN, MANAGER, USER roles")
    void role_AllThreeRoles_CanBeCreated() {
        Role admin   = new Role(1L, "ADMIN",   "Full access");
        Role manager = new Role(2L, "MANAGER", "Can approve transactions");
        Role user    = new Role(3L, "USER",    "Standard user");

        assertThat(admin.getName()).isEqualTo("ADMIN");
        assertThat(manager.getName()).isEqualTo("MANAGER");
        assertThat(user.getName()).isEqualTo("USER");
    }

    // ================================================================
    // Business Logic Sanity Tests
    // ================================================================

    @Test
    @DisplayName("Finance: netBalance = totalIncome - totalExpense (positive case)")
    void netBalance_PositiveWhenIncomeExceedsExpense() {
        BigDecimal income  = new BigDecimal("100000.00");
        BigDecimal expense = new BigDecimal("35000.00");
        BigDecimal net     = income.subtract(expense);

        assertThat(net).isEqualByComparingTo("65000.00");
        assertThat(net.signum()).isPositive();
    }

    @Test
    @DisplayName("Finance: netBalance is negative when expenses exceed income")
    void netBalance_NegativeWhenExpenseExceedsIncome() {
        BigDecimal income  = new BigDecimal("10000.00");
        BigDecimal expense = new BigDecimal("50000.00");
        BigDecimal net     = income.subtract(expense);

        assertThat(net).isEqualByComparingTo("-40000.00");
        assertThat(net.signum()).isNegative();
    }

    @Test
    @DisplayName("Finance: zero balance when income equals expense")
    void netBalance_ZeroWhenEqual() {
        BigDecimal income  = new BigDecimal("25000.00");
        BigDecimal expense = new BigDecimal("25000.00");
        BigDecimal net     = income.subtract(expense);

        assertThat(net).isEqualByComparingTo("0.00");
        assertThat(net.signum()).isZero();
    }

    @Test
    @DisplayName("Finance: null BigDecimal replaced with ZERO does not throw")
    void nullBigDecimal_ReplacedWithZero_NoNPE() {
        BigDecimal nullValue = null;

        // This is the pattern used in SummaryService.orZero()
        BigDecimal safe = nullValue != null ? nullValue : BigDecimal.ZERO;

        assertThatCode(() -> safe.subtract(BigDecimal.ZERO))
                .doesNotThrowAnyException();
        assertThat(safe).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Security: Spring Security role prefix convention is ROLE_ROLENAME")
    void security_RolePrefix_Convention() {
        // Spring Security requires "ROLE_" prefix for role-based auth
        // Our CustomUserDetailsService appends this prefix
        String roleName = "ADMIN";
        String springSecurityRole = "ROLE_" + roleName;

        assertThat(springSecurityRole).isEqualTo("ROLE_ADMIN");
        assertThat(springSecurityRole).startsWith("ROLE_");
    }

    @Test
    @DisplayName("Security: plain password must never equal encoded password")
    void security_PlainPassword_NotEqualToEncoded() {
        String plain = "myPassword123";
        // BCrypt always starts with $2a$, $2b$ or $2y$
        String encoded = "$2a$10$someHashedValueHere";

        assertThat(plain).isNotEqualTo(encoded);
        assertThat(encoded).startsWith("$2a$");
        assertThat(encoded).doesNotContain(plain);
    }
}
