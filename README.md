# Banking Simulation — React + Vite + Spring Boot + Supabase PostgreSQL

A full-stack banking simulation split into a lightweight React/Vite client and a Spring Boot REST API server.

## Stack

- **Client:** React 18 + Vite 5
- **Server:** Spring Boot 3.2 + Java 17
- **Database:** Supabase PostgreSQL
- **ORM:** Spring Data JPA / Hibernate
- **Authentication:** BCrypt password hashing + JWT
- **API docs:** Springdoc / Swagger UI

## Project structure

```text
banking-simulation-full/
├── .env                         # local secrets/config — do not commit
├── .env.example
├── client/
│   ├── .env
│   ├── src/
│   ├── package.json
│   └── vite.config.js
└── server/
    ├── src/main/java/com/bankingsim/
    ├── src/main/resources/application.properties
    └── pom.xml
```


## Authentication flow

1. User registers with username/password.
2. Password is BCrypt-hashed on the server.
3. User logs in.
4. Server signs a JWT with `JWT_SECRET` from `.env`.
5. Client stores the JWT in `sessionStorage`.
6. API calls use `Authorization: Bearer <token>`.
7. Server validates the token before protected banking operations.

## Banking simulation features

- **Strong OOP Domain Architecture:**
  - **Encapsulated Invariants:** `Account` controls its balance mutations (`deposit()`, `withdraw()`, `freeze()`, `unfreeze()`, `close()`), rejecting illegal mutations directly at entity boundaries.
  - **Polymorphic Account Policies:**
    - **Savings Account:** Enforces a minimum balance of **₹500.00**; overdrafts are strictly prevented.
    - **Checking Account:** Zero minimum balance requirement; provides an overdraft buffer up to **₹5,000.00**.
  - **Account Lifecycle & Invariant Rules:**
    - `ACTIVE`: Standard deposits and withdrawals allowed according to policy.
    - `FROZEN`: Permits deposits (incoming funds) but blocks withdrawals and outbound transfers.
    - `CLOSED`: Permanent retirement of account once balance is verified to be ₹0.00.
- **Full CRUD Lifecycle Operations:**
  - **User CRUD:** Registration, profile inspection, role toggling (`ROLE_CUSTOMER` / `ROLE_ADMIN`), user suspension/activation.
  - **Account CRUD:** Account opening, balance inquiry, status updates (Freeze/Unfreeze/Close), ownership validation.
  - **Transaction & Activity CRUD:** Append-only ledger recording deposits, withdrawals, and inter-account transfers (`TRANSFER_OUT` / `TRANSFER_IN`).
- **Enterprise Admin Role & Supervision Suite:**
  - **Overview & Statistics:** Total registered users, total accounts (Active/Frozen/Closed), system liquidity (sum of active balances), total transaction counts.
  - **User Management:** Searchable user table with one-click Suspend/Activate status toggle and Promote/Demote admin role controls. Suspended users are blocked from authentication.
  - **Account Supervision:** Searchable bank-wide account registry with ability to Freeze/Unfreeze any account or Close zero-balance accounts.
  - **Global Activity & Audit Ledger:** Real-time chronological audit trail of all transactions across all users and accounts.
- **Auto-Seeded Administrator:**
  - **Username:** `admin`
  - **Password:** `Admin@123456`
  - Automatically provisioned on server boot if no admin exists.

## Security note

This is a **simulation**, not production banking software. For production financial systems, add multi-factor authentication, refresh-token rotation, distributed transaction locking, idempotency keys, fraud scoring, and formal security compliance.
