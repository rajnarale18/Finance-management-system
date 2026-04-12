# 💰 Finance Data Processing & Access Control Backend

A Spring Boot REST API backend for managing financial transactions with **Role-Based Access Control (RBAC)**.

---

## 🛠 Tech Stack

| Technology       | Purpose                        |
|------------------|-------------------------------|
| Java 17          | Programming language           |
| Spring Boot 3.2  | Backend framework              |
| Spring Security  | Authentication & Authorization |
| Spring Data JPA  | Database ORM                   |
| Hibernate        | JPA implementation             |
| MySQL            | Relational database            |
| Lombok           | Reduce boilerplate code        |
| Maven            | Dependency management          |

---

## 🚀 How to Run the Project

### Step 1: Set Up MySQL
Make sure MySQL is running on your machine, then:
```sql
CREATE DATABASE finance_db;
```
> The app will auto-create all tables on first run.

### Step 2: Configure Database
Open `src/main/resources/application.properties` and update:
```properties
spring.datasource.username=root     ← your MySQL username
spring.datasource.password=root     ← your MySQL password
```

### Step 3: Run the App
```bash
mvn spring-boot:run
```
OR run `FinanceApplication.java` directly from your IDE (IntelliJ / Eclipse).

The server starts at: **http://localhost:8080**

---

## 🔐 Default Login Credentials (Auto-seeded)

| Role    | Username  | Password    |
|---------|-----------|-------------|
| ADMIN   | admin     | admin123    |
| MANAGER | manager   | manager123  |
| USER    | user1     | user123     |
| USER    | user2     | user123     |

---

## 🔑 Role Permissions

| Endpoint                      | USER | MANAGER | ADMIN |
|-------------------------------|------|---------|-------|
| Create transaction            | ✅   | ✅      | ✅    |
| View own transactions         | ✅   | ✅      | ✅    |
| View ALL transactions         | ❌   | ✅      | ✅    |
| Approve/Reject transactions   | ❌   | ✅      | ✅    |
| View overall summary          | ❌   | ✅      | ✅    |
| View own summary              | ✅   | ✅      | ✅    |
| Create/Update/Delete users    | ❌   | ❌      | ✅    |

---

## 📡 API Endpoints

### 👤 User Management (`/api/users`) — ADMIN only

| Method | URL              | Description         |
|--------|------------------|---------------------|
| POST   | /api/users       | Create a new user   |
| GET    | /api/users       | Get all users       |
| GET    | /api/users/{id}  | Get user by ID      |
| PUT    | /api/users/{id}  | Update user         |
| DELETE | /api/users/{id}  | Deactivate user     |

#### Create User Request Body:
```json
{
  "username": "newuser",
  "email": "newuser@example.com",
  "password": "pass123",
  "fullName": "New User",
  "roleName": "USER"
}
```

---

### 💳 Transactions (`/api/transactions`)

| Method | URL                              | Role           | Description              |
|--------|----------------------------------|----------------|--------------------------|
| POST   | /api/transactions/user/{userId}  | All            | Create transaction       |
| GET    | /api/transactions                | MANAGER, ADMIN | Get all transactions     |
| GET    | /api/transactions/{id}           | All            | Get by ID                |
| GET    | /api/transactions/user/{userId}  | All            | Get by user              |
| GET    | /api/transactions/filter/type    | All            | Filter by type           |
| GET    | /api/transactions/filter/status  | MANAGER, ADMIN | Filter by status         |
| GET    | /api/transactions/filter/date    | All            | Filter by date range     |
| PATCH  | /api/transactions/{id}/status    | MANAGER, ADMIN | Approve/Reject           |
| DELETE | /api/transactions/{id}           | ADMIN          | Delete transaction       |

#### Create Transaction Request Body:
```json
{
  "type": "INCOME",
  "amount": 50000.00,
  "currency": "INR",
  "category": "Salary",
  "description": "April salary",
  "transactionDate": "2024-04-01"
}
```

#### Approve/Reject Request Body:
```json
{
  "status": "APPROVED"
}
```

#### Filter Examples:
```
GET /api/transactions/filter/type?type=INCOME
GET /api/transactions/filter/status?status=PENDING
GET /api/transactions/filter/date?start=2024-04-01&end=2024-04-30
```

---

### 📊 Summary (`/api/summary`)

| Method | URL                        | Role           | Description              |
|--------|----------------------------|----------------|--------------------------|
| GET    | /api/summary/overall       | MANAGER, ADMIN | Full financial summary   |
| GET    | /api/summary/user/{userId} | All            | Per-user summary         |

#### Sample Summary Response:
```json
{
  "totalIncome": 95000.00,
  "totalExpense": 20000.00,
  "netBalance": 75000.00,
  "pendingTransactions": 2,
  "approvedTransactions": 3,
  "rejectedTransactions": 1,
  "categoryBreakdown": {
    "Salary": 95000.00,
    "Rent": 12000.00,
    "Transport": 8000.00
  }
}
```

---

## 🧪 Testing with Postman

1. Open Postman
2. Set request URL (e.g., `GET http://localhost:8080/api/transactions`)
3. Go to **Authorization** tab → Select **Basic Auth**
4. Enter username and password (from table above)
5. Click **Send**

---

## 📁 Project Structure

```
finance-backend/
├── pom.xml
└── src/main/java/com/finance/
    ├── FinanceApplication.java          ← App entry point
    ├── config/
    │   ├── SecurityConfig.java          ← Spring Security setup
    │   └── CustomUserDetailsService.java← Loads user from DB for auth
    ├── model/
    │   ├── Role.java                    ← Role entity (ADMIN/MANAGER/USER)
    │   ├── User.java                    ← User entity
    │   └── Transaction.java             ← Transaction entity
    ├── repository/
    │   ├── RoleRepository.java
    │   ├── UserRepository.java
    │   └── TransactionRepository.java   ← Includes JPQL summary queries
    ├── dto/
    │   ├── UserDTO.java                 ← Request/Response objects for User
    │   ├── TransactionDTO.java          ← Request/Response objects for Transaction
    │   └── SummaryDTO.java              ← Summary report object
    ├── service/
    │   ├── UserService.java             ← User business logic
    │   ├── TransactionService.java      ← Transaction business logic
    │   └── SummaryService.java          ← Summary/report logic
    ├── controller/
    │   ├── UserController.java          ← /api/users endpoints
    │   ├── TransactionController.java   ← /api/transactions endpoints
    │   └── SummaryController.java       ← /api/summary endpoints
    └── seeder/
        └── DataSeeder.java              ← Auto-seeds roles, users, transactions
```
