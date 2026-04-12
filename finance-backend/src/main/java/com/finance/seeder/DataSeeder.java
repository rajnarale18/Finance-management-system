package com.finance.seeder;

import com.finance.model.Role;
import com.finance.model.Transaction;
import com.finance.model.Transaction.TransactionStatus;
import com.finance.model.Transaction.TransactionType;
import com.finance.model.User;
import com.finance.repository.RoleRepository;
import com.finance.repository.TransactionRepository;
import com.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DataSeeder - Automatically seeds initial data when the application starts.
 *
 * It implements CommandLineRunner, which means Spring Boot will automatically
 * call the run() method after the application starts up.
 *
 * This seeds:
 * 1. Roles: ADMIN, MANAGER, USER
 * 2. Users: one of each role
 * 3. Sample Transactions: a few sample income/expense records
 *
 * Default Credentials:
 * - admin   / admin123   (ADMIN role)
 * - manager / manager123 (MANAGER role)
 * - user1   / user123    (USER role)
 */
@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        System.out.println("🌱 Starting Data Seeding...");

        // ---- Step 1: Seed Roles ----
        Role adminRole = seedRole("ADMIN", "Full access to everything");
        Role managerRole = seedRole("MANAGER", "Can view and approve transactions");
        Role userRole = seedRole("USER", "Can create and view own transactions");

        // ---- Step 2: Seed Users ----
        seedUser("admin", "admin@finance.com", "admin123",
                              "Admin User", adminRole);

        seedUser("manager", "manager@finance.com", "manager123",
                                "Finance Manager", managerRole);

        User user1 = seedUser("user1", "user1@finance.com", "user123",
                              "John Doe", userRole);

        User user2 = seedUser("user2", "user2@finance.com", "user123",
                              "Jane Smith", userRole);

        // ---- Step 3: Seed Sample Transactions (only if table is empty) ----
        if (transactionRepository.count() == 0) {
        seedTransaction(user1, TransactionType.INCOME, new BigDecimal("50000.00"),
                        "INR", "Salary", "Monthly salary - April", "2024-04-01",
                        TransactionStatus.APPROVED);

        seedTransaction(user1, TransactionType.EXPENSE, new BigDecimal("12000.00"),
                        "INR", "Rent", "Monthly apartment rent", "2024-04-02",
                        TransactionStatus.APPROVED);

        seedTransaction(user1, TransactionType.EXPENSE, new BigDecimal("3500.00"),
                        "INR", "Food", "Grocery and dining expenses", "2024-04-05",
                        TransactionStatus.PENDING);

        seedTransaction(user2, TransactionType.INCOME, new BigDecimal("45000.00"),
                        "INR", "Salary", "Monthly salary - April", "2024-04-01",
                        TransactionStatus.APPROVED);

        seedTransaction(user2, TransactionType.EXPENSE, new BigDecimal("8000.00"),
                        "INR", "Transport", "Cab and fuel expenses", "2024-04-10",
                        TransactionStatus.PENDING);

        seedTransaction(user2, TransactionType.EXPENSE, new BigDecimal("2000.00"),
                        "INR", "Utilities", "Electricity and internet bill", "2024-04-12",
                        TransactionStatus.REJECTED);

        } // end if count == 0

        System.out.println("✅ Data Seeding Complete!");
        System.out.println("--------------------------------------------------");
        System.out.println("🔐 Default Login Credentials:");
        System.out.println("   Admin   → username: admin    | password: admin123");
        System.out.println("   Manager → username: manager  | password: manager123");
        System.out.println("   User    → username: user1    | password: user123");
        System.out.println("--------------------------------------------------");
    }

    // ---- Helper: Create a role if it doesn't already exist ----
    private Role seedRole(String name, String description) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            Role saved = roleRepository.save(role);
            System.out.println("  ✔ Role created: " + name);
            return saved;
        });
    }

    // ---- Helper: Create a user if username doesn't already exist ----
    private User seedUser(String username, String email, String password,
                          String fullName, Role role) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password)); // Always encrypt!
            user.setFullName(fullName);
            user.setRole(role);
            user.setActive(true);
            User saved = userRepository.save(user);
            System.out.println("  ✔ User created: " + username + " [" + role.getName() + "]");
            return saved;
        });
    }

    // ---- Helper: Create a transaction (called only when table is empty) ----
    private void seedTransaction(User user, TransactionType type, BigDecimal amount,
                                  String currency, String category, String description,
                                  String date, TransactionStatus status) {
        Transaction t = new Transaction();
        t.setUser(user);
        t.setType(type);
        t.setAmount(amount);
        t.setCurrency(currency);
        t.setCategory(category);
        t.setDescription(description);
        t.setTransactionDate(LocalDate.parse(date));
        t.setStatus(status);
        transactionRepository.save(t);
    }
}
