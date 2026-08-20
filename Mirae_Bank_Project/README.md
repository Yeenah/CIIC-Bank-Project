# Mirae Bank

## Project structure

```text
Mirae_Banking_System/
│
└── Mirae_Bank_Project/
    │
    ├── pom.xml
    ├── README.md
    │
    ├── sql/
    │   └── banking_schema.sql
    │
    └── src/
        │
        └── main/
            │
            ├── java/
            │   │
            │   └── com/
            │       └── bank/
            │           │
            │           ├── api/
            │           │   └── BankApiServer.java
            │           │
            │           ├── appEntry/
            │           │   └── Main.java
            │           │
            │           ├── auth/
            │           │   ├── AuthDAO.java
            │           │   ├── AuthService.java
            │           │   └── PasswordUtil.java
            │           │
            │           ├── config/
            │           │   ├── DBConnection.java
            │           │   └── PropertyLoader.java
            │           │
            │           ├── dao/
            │           │   ├── AccountDAO.java
            │           │   ├── TransactionDAO.java
            │           │   │
            │           │   └── implementation/
            │           │       ├── AccountDAOImpl.java
            │           │       └── TransactionDAOImpl.java
            │           │
            │           ├── exception/
            │           │   ├── AccountNotFoundException.java
            │           │   ├── InsufficientBalanceException.java
            │           │   └── InvalidTransactionException.java
            │           │
            │           ├── manager/
            │           │   ├── AccountManager.java
            │           │   └── TransactionManager.java
            │           │
            │           ├── menu/
            │           │   └── BankDashboard.java
            │           │
            │           ├── model/
            │           │   ├── Account.java
            │           │   ├── Transaction.java
            │           │   └── TransactionType.java
            │           │
            │           ├── service/
            │           │   └── TransactionService.java
            │           │
            │           └── util/
            │               └── ReferenceNumberGenerator.java
            │
            │
            └── resources/
                │
                ├── application.properties
                │
                └── frontend/
                    │
                    ├── Assets/
                    │   └── images/
                    │       ├── cha-eun-woo.png
                    │       └── mirae logo.png
                    │
                    ├── css/
                    │   ├── dashboard.css
                    │   ├── signin.css
                    │   └── signup.css
                    │
                    ├── html/
                    │   ├── dashboard.html
                    │   ├── signin.html
                    │   └── signup.html
                    │
                    ├── js/
                    │   ├── dashboard.js
                    │   ├── signin.js
                    │   └── signup.js
                    │
                    └── README.txt
```

## Running

Start `com.bank.appEntry.Main`. The server listens on port 8082.

Open:

`http://localhost:8082/`


