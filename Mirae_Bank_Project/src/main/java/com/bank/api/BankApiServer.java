package com.bank.api;

import com.bank.auth.AuthService;
import com.bank.dao.implementation.AccountDAOImpl;
import com.bank.dao.implementation.TransactionDAOImpl;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.InsufficientBalanceException;
import com.bank.exception.InvalidTransactionException;
import com.bank.exception.AuthenticationException;
import com.bank.exception.UnauthorizedException;
import com.bank.exception.DuplicateAccountException;
import com.bank.exception.InvalidAccountException;
import com.bank.exception.InvalidAmountException;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.service.TransactionService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BankApiServer {

    private static final Logger LOGGER =
            Logger.getLogger(BankApiServer.class.getName());

    private static final int PORT = 8082;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AuthService authService =
            new AuthService();

    private final AccountDAOImpl accountDAO =
            new AccountDAOImpl();

    private final TransactionDAOImpl transactionDAO =
            new TransactionDAOImpl();

    private final TransactionService transactionService =
            new TransactionService(
                    accountDAO,
                    transactionDAO
            );

    private final Map<String, String> sessions =
            new ConcurrentHashMap<>();

    public void start() throws IOException {

        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(PORT),
                        0
                );

        server.createContext(
                "/api/auth/login",
                this::handleLogin
        );

        server.createContext(
                "/api/auth/signup",
                this::handleSignup
        );

        server.createContext(
                "/api/auth/logout",
                this::handleLogout
        );

        server.createContext(
                "/api/account",
                this::handleAccount
        );

        server.createContext(
                "/api/accounts",
                this::handleAccounts
        );

        server.createContext(
                "/api/account/create",
                this::handleCreateAccount
        );

        server.createContext(
                "/api/transactions",
                this::handleTransactions
        );

        server.createContext(
                "/api/transactions/deposit",
                this::handleDeposit
        );

        server.createContext(
                "/api/transactions/withdraw",
                this::handleWithdraw
        );

        server.createContext(
                "/api/transactions/transfer",
                this::handleTransfer
        );

        server.createContext(
                "/",
                this::handleFrontend
        );

        server.setExecutor(
                Executors.newFixedThreadPool(8)
        );

        server.start();

        LOGGER.info(
                "Mirae Bank server started at:"
        );

        LOGGER.info(
                "http://localhost:" + PORT
        );

        URL signinResource =
                BankApiServer.class.getResource(
                        "/frontend/html/signin.html"
                );

        URL dashboardResource =
                BankApiServer.class.getResource(
                        "/frontend/html/dashboard.html"
                );

        LOGGER.info(
                "Frontend signin.html resource: "
                        + signinResource
        );

        LOGGER.info(
                "Frontend dashboard.html resource: "
                        + dashboardResource
        );
    }

    private void handleLogin(
            HttpExchange exchange
    ) throws IOException {

        if (handleOptions(exchange)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendJson(
                    exchange,
                    405,
                    "{\"message\":\"Method not allowed.\"}"
            );

            return;
        }

        try {

            String body =
                    readBody(exchange);

            String username =
                    jsonValue(
                            body,
                            "username"
                    );

            String password =
                    jsonValue(
                            body,
                            "password"
                    );

            if (username == null ||
                    username.isBlank() ||
                    password == null ||
                    password.isBlank()) {

                sendJson(
                        exchange,
                        400,
                        "{\"message\":\"Username and password are required.\"}"
                );

                return;
            }

            AuthService.LoginResult login =
                    authService.login(
                            username,
                            password
                    );

            Account account =
                    login.account();

            String token =
                    UUID.randomUUID().toString();

            sessions.put(
                    token,
                    account.getAccountNumber()
            );

            printSessions();

            String json =
                    "{"
                            + "\"message\":\"Login successful\","
                            + "\"token\":"
                            + quote(token)
                            + ","
                            + "\"username\":"
                            + quote(login.username())
                            + ","
                            + "\"accountNumber\":"
                            + quote(account.getAccountNumber())
                            + ","
                            + "\"accountName\":"
                            + quote(account.getAccountName())
                            + ","
                            + "\"balance\":"
                            + account.getBalance().toPlainString()
                            + "}";

            sendJson(
                    exchange,
                    200,
                    json
            );

        } catch (AuthenticationException e) {

            sendJson(
                    exchange,
                    401,
                    "{\"message\":" + quote(e.getMessage()) + "}"
            );

        } catch (Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "Login API error",
                    e
            );

            sendJson(
                    exchange,
                    500,
                    "{\"message\":\"Server error while processing login.\"}"
            );
        }
    }

    private void handleSignup(
            HttpExchange exchange
    ) throws IOException {

        if (handleOptions(exchange)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendJson(
                    exchange,
                    405,
                    "{\"message\":\"Method not allowed.\"}"
            );

            return;
        }

        try {

            String body =
                    readBody(exchange);

            String fullName =
                    jsonValue(
                            body,
                            "fullName"
                    );

            String username =
                    jsonValue(
                            body,
                            "username"
                    );

            String email =
                    jsonValue(
                            body,
                            "email"
                    );

            String password =
                    jsonValue(
                            body,
                            "password"
                    );

            if (fullName == null ||
                    fullName.isBlank()) {

                sendJson(
                        exchange,
                        400,
                        "{\"message\":\"Full name is required.\"}"
                );

                return;
            }

            if (username == null ||
                    username.isBlank()) {

                sendJson(
                        exchange,
                        400,
                        "{\"message\":\"Username is required.\"}"
                );

                return;
            }

            if (password == null ||
                    password.isBlank()) {

                sendJson(
                        exchange,
                        400,
                        "{\"message\":\"Password is required.\"}"
                );

                return;
            }

            if (password.length() < 6) {

                sendJson(
                        exchange,
                        400,
                        "{\"message\":\"Password must be at least 6 characters.\"}"
                );

                return;
            }

            AuthService.SignupResult result =
                    authService.signup(
                            fullName,
                            username,
                            email,
                            password
                    );

            String json =
                    "{"
                            + "\"message\":\"Profile and bank account created successfully.\","
                            + "\"username\":"
                            + quote(result.username())
                            + ","
                            + "\"fullName\":"
                            + quote(result.fullName())
                            + ","
                            + "\"accountNumber\":"
                            + quote(result.accountNumber())
                            + ","
                            + "\"balance\":0.00"
                            + "}";

            sendJson(
                    exchange,
                    201,
                    json
            );

        } catch (InvalidAccountException e) {

            sendJson(
                    exchange,
                    400,
                    "{\"message\":"
                            + quote(e.getMessage())
                            + "}"
            );

        } catch (DuplicateAccountException e) {

            sendJson(
                    exchange,
                    409,
                    "{\"message\":"
                            + quote(e.getMessage())
                            + "}"
            );

        } catch (
                java.sql.SQLIntegrityConstraintViolationException e
        ) {

            LOGGER.log(
                    Level.WARNING,
                    "Signup database constraint error",
                    e
            );

            sendJson(
                    exchange,
                    409,
                    "{\"message\":\"Username already exists or account data conflicts with the database.\"}"
            );

        } catch (Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "Signup API error",
                    e
            );

            sendJson(
                    exchange,
                    500,
                    "{\"message\":\"Unable to create profile.\"}"
            );
        }
    }

    private void handleLogout(
            HttpExchange exchange
    ) throws IOException {

        if (handleOptions(exchange)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendJson(
                    exchange,
                    405,
                    "{\"message\":\"Method not allowed.\"}"
            );

            return;
        }

        String token =
                bearerToken(exchange);

        if (token != null) {

            sessions.remove(token);

            printSessions();
        }

        sendJson(
                exchange,
                200,
                "{\"message\":\"Logged out successfully.\"}"
        );
    }

    private void handleAccount(
            HttpExchange exchange
    ) throws IOException {

        if (handleOptions(exchange)) {
            return;
        }

        if (!"GET".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendJson(
                    exchange,
                    405,
                    "{\"message\":\"Method not allowed.\"}"
            );

            return;
        }

        String accountNumber =
                authenticatedAccount(exchange);

        if (accountNumber == null) {
            return;
        }

        try {

            Optional<Account> account =
                    accountDAO.findByAccountNumber(
                            accountNumber
                    );

            if (account.isEmpty()) {

                sendJson(
                        exchange,
                        404,
                        "{\"message\":\"Account not found.\"}"
                );

                return;
            }

            Account a =
                    account.get();

            String json =
                    "{"
                            + "\"accountNumber\":"
                            + quote(a.getAccountNumber())
                            + ","
                            + "\"accountName\":"
                            + quote(a.getAccountName())
                            + ","
                            + "\"balance\":"
                            + a.getBalance().toPlainString()
                            + "}";

            sendJson(
                    exchange,
                    200,
                    json
            );

        } catch (Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "Account API error",
                    e
            );

            sendJson(
                    exchange,
                    500,
                    "{\"message\":\"Unable to load account.\"}"
            );
        }
    }

    private void handleAccounts(
            HttpExchange exchange
    ) throws IOException {

        if (handleOptions(exchange)) {
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(
                    exchange,
                    405,
                    "{\"message\":\"Method not allowed.\"}"
            );
            return;
        }

        try {
            List<Account> accounts = accountDAO.findAllAccounts();

            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < accounts.size(); i++) {
                if (i > 0) {
                    json.append(",");
                }

                Account account = accounts.get(i);

                json.append("{")
                        .append("\"accountNumber\":")
                        .append(quote(account.getAccountNumber()))
                        .append(",")
                        .append("\"accountName\":")
                        .append(quote(account.getAccountName()))
                        .append(",")
                        .append("\"balance\":")
                        .append(account.getBalance().toPlainString())
                        .append("}");
            }

            json.append("]");

            sendJson(
                    exchange,
                    200,
                    json.toString()
            );

        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Unable to load accounts",
                    e
            );

            sendJson(
                    exchange,
                    500,
                    "{\"message\":\"Unable to load accounts.\"}"
            );
        }
    }

    private void handleCreateAccount(
            HttpExchange exchange
    ) throws IOException {

        if (handleOptions(exchange)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405,
                    "{\"message\":\"Method not allowed.\"}");
            return;
        }

        try {

            String body = readBody(exchange);

            String accountNumber =
                    jsonValue(body, "accountNumber");

            String accountName =
                    jsonValue(body, "accountName");

            String initialDeposit =
                    jsonValue(body, "initialDeposit");

            if (accountNumber == null || accountNumber.isBlank()) {
                throw new InvalidAccountException(
                        "Account number is required."
                );
            }

            if (accountName == null || accountName.isBlank()) {
                throw new InvalidAccountException(
                        "Account holder name is required."
                );
            }

            BigDecimal amount =
                    new BigDecimal(initialDeposit);

            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidAmountException(
                        "Initial deposit must be zero or greater."
                );
            }

            if (accountDAO.findByAccountNumber(accountNumber).isPresent()) {
                throw new DuplicateAccountException(
                        "Account number already exists."
                );
            }

            Account account =
                    new Account(
                            accountNumber,
                            accountName,
                            amount
                    );

            accountDAO.createAccount(account);

            sendJson(
                    exchange,
                    201,
                    "{"
                            + "\"message\":\"Account created successfully\","
                            + "\"accountNumber\":"+quote(accountNumber)+","
                            + "\"accountName\":"+quote(accountName)+","
                            + "\"balance\":"+amount.toPlainString()
                            + "}"
            );

        } catch (InvalidAccountException | InvalidAmountException e) {

            sendJson(
                    exchange,
                    400,
                    "{\"message\":" + quote(e.getMessage()) + "}"
            );

        } catch (DuplicateAccountException e) {

            sendJson(
                    exchange,
                    409,
                    "{\"message\":" + quote(e.getMessage()) + "}"
            );

        } catch(Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "Create account error",
                    e
            );

            sendJson(
                    exchange,
                    500,
                    "{\"message\":\"Unable to create account.\"}"
            );
        }
    }

    private void handleTransactions(
            HttpExchange exchange
    ) throws IOException {

        if (handleOptions(exchange)) {
            return;
        }

        if (!"GET".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendJson(
                    exchange,
                    405,
                    "{\"message\":\"Method not allowed.\"}"
            );

            return;
        }

        String accountNumber =
                authenticatedAccount(exchange);

        if (accountNumber == null) {
            return;
        }

        try {

            List<Transaction> transactions =
                    transactionService.findRecentTransactions(
                            accountNumber,
                            10
                    );

            sendJson(
                    exchange,
                    200,
                    transactionsToJson(transactions)
            );

        } catch (AccountNotFoundException e) {

            sendJson(
                    exchange,
                    404,
                    "{\"message\":"
                            + quote(e.getMessage())
                            + "}"
            );

        } catch (InvalidTransactionException e) {

            sendJson(
                    exchange,
                    400,
                    "{\"message\":"
                            + quote(e.getMessage())
                            + "}"
            );

        } catch (Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "Transactions API error",
                    e
            );

            sendJson(
                    exchange,
                    500,
                    "{\"message\":\"Unable to load transactions.\"}"
            );
        }
    }

    private void handleDeposit(
            HttpExchange exchange
    ) throws IOException {

        if (handleOptions(exchange)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendJson(
                    exchange,
                    405,
                    "{\"message\":\"Method not allowed.\"}"
            );

            return;
        }

        String accountNumber =
                authenticatedAccount(exchange);

        if (accountNumber == null) {
            return;
        }

        try {

            String body =
                    readBody(exchange);

            String amountString =
                    jsonValue(
                            body,
                            "amount"
                    );

            if (amountString == null ||
                    amountString.isBlank()) {

                sendJson(
                        exchange,
                        400,
                        "{\"message\":\"Amount is required.\"}"
                );

                return;
            }

            BigDecimal amount;

            try {

                amount =
                        new BigDecimal(
                                amountString
                        );

             } catch (NumberFormatException e) {

                throw new InvalidAmountException(
                        "Invalid amount.",
                        e
                );
            }

            Transaction transaction =
                    transactionService.deposit(
                            accountNumber,
                            amount
                    );

            String json =
                    "{"
                            + "\"message\":\"Deposit successful\","
                            + "\"transaction\":"
                            + transactionToJson(transaction)
                            + "}";

            sendJson(
                    exchange,
                    200,
                    json
            );

        } catch (AccountNotFoundException e) {

            sendJson(
                    exchange,
                    404,
                    "{\"message\":"
                            + quote(e.getMessage())
                            + "}"
            );

        } catch (InvalidAmountException e) {

            sendJson(
                    exchange,
                    400,
                    "{\"message\":" + quote(e.getMessage()) + "}"
            );

        } catch (InvalidTransactionException e) {

            sendJson(
                    exchange,
                    400,
                    "{\"message\":"
                            + quote(e.getMessage())
                            + "}"
            );

        } catch (Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "Deposit API error",
                    e
            );

            sendJson(
                    exchange,
                    500,
                    "{\"message\":\"Deposit failed.\"}"
            );
        }
    }

    private void handleWithdraw(
            HttpExchange exchange
    ) throws IOException {

        if (handleOptions(exchange)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendJson(
                    exchange,
                    405,
                    "{\"message\":\"Method not allowed.\"}"
            );

            return;
        }

        String accountNumber =
                authenticatedAccount(exchange);

        if (accountNumber == null) {
            return;
        }

        try {

            String body =
                    readBody(exchange);

            String amountString =
                    jsonValue(
                            body,
                            "amount"
                    );

            if (amountString == null ||
                    amountString.isBlank()) {

                sendJson(
                        exchange,
                        400,
                        "{\"message\":\"Amount is required.\"}"
                );

                return;
            }

            BigDecimal amount;

            try {

                amount =
                        new BigDecimal(
                                amountString
                        );

             } catch (NumberFormatException e) {

                throw new InvalidAmountException(
                        "Invalid amount.",
                        e
                );
            }

            Transaction transaction =
                    transactionService.withdraw(
                            accountNumber,
                            amount
                    );

            String json =
                    "{"
                            + "\"message\":\"Withdrawal successful\","
                            + "\"transaction\":"
                            + transactionToJson(transaction)
                            + "}";

            sendJson(
                    exchange,
                    200,
                    json
            );

        } catch (AccountNotFoundException e) {

            sendJson(
                    exchange,
                    404,
                    "{\"message\":"
                            + quote(e.getMessage())
                            + "}"
            );

        } catch (InsufficientBalanceException e) {

            sendJson(
                    exchange,
                    400,
                    "{\"message\":"
                            + quote(e.getMessage())
                            + "}"
            );

        } catch (InvalidAmountException e) {

            sendJson(
                    exchange,
                    400,
                    "{\"message\":" + quote(e.getMessage()) + "}"
            );

        } catch (InvalidTransactionException e) {

            sendJson(
                    exchange,
                    400,
                    "{\"message\":"
                            + quote(e.getMessage())
                            + "}"
            );

        } catch (Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "Withdrawal API error",
                    e
            );

            sendJson(
                    exchange,
                    500,
                    "{\"message\":\"Withdrawal failed.\"}"
            );
        }
    }

    private void handleTransfer(
            HttpExchange exchange
    ) throws IOException {

        if (handleOptions(exchange)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendJson(
                    exchange,
                    405,
                    "{\"message\":\"Method not allowed.\"}"
            );

            return;
        }

        String authenticatedAccount =
                authenticatedAccount(exchange);

        if (authenticatedAccount == null) {
            return;
        }

        try {

            String body =
                    readBody(exchange);

            String receiverAccount =
                    jsonValue(
                            body,
                            "receiverAccountNumber"
                    );

            String amountString =
                    jsonValue(
                            body,
                            "amount"
                    );

            if (receiverAccount == null ||
                    receiverAccount.isBlank()) {

                sendJson(
                        exchange,
                        400,
                        "{\"message\":\"Receiver account number is required.\"}"
                );

                return;
            }

            if (amountString == null ||
                    amountString.isBlank()) {

                sendJson(
                        exchange,
                        400,
                        "{\"message\":\"Amount is required.\"}"
                );

                return;
            }

            BigDecimal amount;

            try {

                amount =
                        new BigDecimal(
                                amountString
                        );

             } catch (NumberFormatException e) {

                throw new InvalidAmountException(
                        "Invalid amount.",
                        e
                );
            }

            TransactionService.TransferResult result =
                    transactionService.transfer(
                            authenticatedAccount,
                            receiverAccount,
                            amount
                    );

            Transaction transferOut =
                    result.transferOut();

            Transaction transferIn =
                    result.transferIn();

            String json =
                    "{"
                            + "\"message\":\"Transfer successful\","
                            + "\"transferOut\":"
                            + transactionToJson(transferOut)
                            + ","
                            + "\"transferIn\":"
                            + transactionToJson(transferIn)
                            + "}";

            sendJson(
                    exchange,
                    200,
                    json
            );

        } catch (AccountNotFoundException e) {

            sendJson(
                    exchange,
                    404,
                    "{\"message\":"
                            + quote(e.getMessage())
                            + "}"
            );

        } catch (InsufficientBalanceException e) {

            sendJson(
                    exchange,
                    400,
                    "{\"message\":"
                            + quote(e.getMessage())
                            + "}"
            );

        } catch (InvalidAmountException e) {

            sendJson(
                    exchange,
                    400,
                    "{\"message\":" + quote(e.getMessage()) + "}"
            );

        } catch (InvalidTransactionException e) {

            sendJson(
                    exchange,
                    400,
                    "{\"message\":"
                            + quote(e.getMessage())
                            + "}"
            );

        } catch (Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "Transfer API error",
                    e
            );

            sendJson(
                    exchange,
                    500,
                    "{\"message\":\"Transfer failed.\"}"
            );
        }
    }

    private String authenticatedAccount(
            HttpExchange exchange
    ) throws IOException {

        String token =
                bearerToken(exchange);

        String accountNumber =
                token == null
                        ? null
                        : sessions.get(token);

        if (accountNumber == null) {

            sendJson(
                    exchange,
                    401,
                    "{\"message\":\"Unauthorized. Please sign in again.\"}"
            );

            return null;
        }

        return accountNumber;
    }

    private static String bearerToken(
            HttpExchange exchange
    ) {

        String auth =
                exchange.getRequestHeaders()
                        .getFirst("Authorization");

        if (auth == null ||
                !auth.startsWith("Bearer ")) {

            return null;
        }

        return auth.substring(
                "Bearer ".length()
        ).trim();
    }

    private void printSessions() {

        System.out.println(
                "\n========== ACTIVE SESSIONS =========="
        );

        if (sessions.isEmpty()) {

            System.out.println(
                    "No active sessions."
            );

        } else {

            sessions.forEach(
                    (token, accountNumber) ->
                            System.out.println(
                                    "Token: "
                                            + token
                                            + " -> Account: "
                                            + accountNumber
                            )
            );
        }

        System.out.println(
                "=====================================\n"
        );
    }

    private static String transactionToJson(
            Transaction t
    ) {

        return "{"
                + "\"reference\":"
                + quote(t.getReferenceNumber())
                + ","

                + "\"type\":"
                + quote(
                t.getTransactionType()
                        .displayTransType()
        )
                + ","

                + "\"amount\":"
                + t.getAmount().toPlainString()
                + ","

                + "\"balanceAfter\":"
                + t.getBalanceAfter().toPlainString()
                + ","

                + "\"remarks\":"
                + quote(t.getRemarks())
                + ","

                + "\"createdAt\":"
                + quote(
                t.getCreatedAt() == null
                        ? null
                        : t.getCreatedAt()
                        .format(DATE_FORMAT)
        )

                + "}";
    }

    private static String transactionsToJson(
            List<Transaction> transactions
    ) {

        StringBuilder json =
                new StringBuilder("[");

        for (int i = 0;
             i < transactions.size();
             i++) {

            if (i > 0) {
                json.append(",");
            }

            json.append(
                    transactionToJson(
                            transactions.get(i)
                    )
            );
        }

        json.append("]");

        return json.toString();
    }

    private void handleFrontend(
            HttpExchange exchange
    ) throws IOException {

        if (handleOptions(exchange)) {
            return;
        }

        String path =
                exchange.getRequestURI()
                        .getPath();

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "Frontend request path: "
                        + path
        );

        if (path.equals("/") || path.equals("/index.html")) {

            path =
                    "/html/signin.html";
        }

        if (path.equals("/signin.html")) {
            path = "/html/signin.html";
        } else if (path.equals("/signup.html")) {
            path = "/html/signup.html";
        } else if (path.equals("/createaccount.html")) {
            path = "/html/createaccount.html";
        } else if (path.equals("/dashboard.html")) {
            path = "/html/dashboard.html";
        }

        if (path.contains("..")) {

            sendJson(
                    exchange,
                    400,
                    "{\"message\":\"Invalid file path.\"}"
            );

            return;
        }
        if (!path.startsWith("/html/")
                && !path.startsWith("/css/")
                && !path.startsWith("/js/")
                && !path.startsWith("/Assets/")) {

            sendJson(
                    exchange,
                    404,
                    "{\"message\":\"Frontend file not found.\"}"
            );

            return;
        }

        String resourcePath =
                "/frontend" + path;

        System.out.println(
                "Frontend resource path: "
                        + resourcePath
        );

        URL resource =
                BankApiServer.class.getResource(
                        resourcePath
                );

        System.out.println(
                "Frontend resource URL: "
                        + resource
        );

        if (resource == null) {

            System.out.println(
                    "WARNING: Resource was NOT found on the classpath."
            );

            System.out.println(
                    "=========================================="
            );

            sendJson(
                    exchange,
                    404,
                    "{\"message\":\"Frontend file not found.\"}"
            );

            return;
        }

        System.out.println(
                "SUCCESS: Resource was found on the classpath."
        );

        System.out.println(
                "=========================================="
        );

        try (InputStream input =
                     BankApiServer.class
                             .getResourceAsStream(
                                     resourcePath
                             )) {

            if (input == null) {

                sendJson(
                        exchange,
                        404,
                        "{\"message\":\"Frontend file not found.\"}"
                );

                return;
            }

            byte[] bytes =
                    input.readAllBytes();

            String contentType =
                    getContentType(path);

            exchange.getResponseHeaders()
                    .set(
                            "Content-Type",
                            contentType
                    );

            exchange.getResponseHeaders()
                    .set(
                            "Cache-Control",
                            "no-cache"
                    );

            addCors(exchange);

            exchange.sendResponseHeaders(
                    200,
                    bytes.length
            );

            try (var output =
                         exchange.getResponseBody()) {

                output.write(bytes);
            }

        } catch (Exception e) {

            LOGGER.log(
                    Level.SEVERE,
                    "Frontend file error: " + path,
                    e
            );

            sendJson(
                    exchange,
                    500,
                    "{\"message\":\"Unable to load frontend file.\"}"
            );
        }
    }

    private static String getContentType(
            String path
    ) {

        String lower =
                path.toLowerCase();

        if (lower.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }

        if (lower.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }

        if (lower.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }

        if (lower.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        }

        if (lower.endsWith(".png")) {
            return "image/png";
        }

        if (lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }

        if (lower.endsWith(".ico")) {
            return "image/x-icon";
        }

        if (lower.endsWith(".webp")) {
            return "image/webp";
        }

        if (lower.endsWith(".gif")) {
            return "image/gif";
        }

        return "application/octet-stream";
    }

    private static boolean handleOptions(
            HttpExchange exchange
    ) throws IOException {

        if (!"OPTIONS".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            return false;
        }

        addCors(exchange);

        exchange.sendResponseHeaders(
                204,
                -1
        );

        exchange.close();

        return true;
    }

    private static String readBody(
            HttpExchange exchange
    ) throws IOException {

        try (InputStream input =
                     exchange.getRequestBody()) {

            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

    private static String jsonValue(
            String json,
            String key
    ) {

        if (json == null ||
                key == null) {

            return null;
        }

        String needle =
                "\"" + key + "\"";

        int keyIndex =
                json.indexOf(needle);

        if (keyIndex < 0) {
            return null;
        }

        int colon =
                json.indexOf(
                        ':',
                        keyIndex + needle.length()
                );

        if (colon < 0) {
            return null;
        }

        int valueStart =
                colon + 1;

        while (
                valueStart < json.length()
                        &&
                        Character.isWhitespace(
                                json.charAt(valueStart)
                        )
        ) {

            valueStart++;
        }

        if (
                valueStart < json.length()
                        &&
                        json.charAt(valueStart) == '"'
        ) {

            int firstQuote =
                    valueStart;

            int endQuote =
                    firstQuote + 1;

            while (endQuote < json.length()) {

                if (
                        json.charAt(endQuote) == '"'
                                &&
                                json.charAt(endQuote - 1) != '\\'
                ) {

                    break;
                }

                endQuote++;
            }

            if (endQuote >= json.length()) {
                return null;
            }

            return json.substring(
                            firstQuote + 1,
                            endQuote
                    )
                    .replace(
                            "\\\"",
                            "\""
                    )
                    .replace(
                            "\\\\",
                            "\\"
                    );
        }

        int end =
                valueStart;

        while (
                end < json.length()
                        &&
                        json.charAt(end) != ','
                        &&
                        json.charAt(end) != '}'
        ) {

            end++;
        }

        String value =
                json.substring(
                        valueStart,
                        end
                ).trim();

        if ("null".equals(value)) {
            return null;
        }

        return value;
    }

    private static String quote(
            String value
    ) {

        if (value == null) {
            return "null";
        }

        return "\""
                + value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\r",
                        "\\r"
                )
                .replace(
                        "\n",
                        "\\n"
                )
                + "\"";
    }

    private static void sendJson(
            HttpExchange exchange,
            int status,
            String body
    ) throws IOException {

        addCors(exchange);

        byte[] bytes =
                body.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

        exchange.sendResponseHeaders(
                status,
                bytes.length
        );

        try (var output =
                     exchange.getResponseBody()) {

            output.write(bytes);
        }
    }

    private static void addCors(
            HttpExchange exchange
    ) {

        exchange.getResponseHeaders()
                .set(
                        "Access-Control-Allow-Origin",
                        "*"
                );

        exchange.getResponseHeaders()
                .set(
                        "Access-Control-Allow-Headers",
                        "Content-Type, Authorization"
                );

        exchange.getResponseHeaders()
                .set(
                        "Access-Control-Allow-Methods",
                        "GET, POST, OPTIONS"
                );
    }

    public static void main(
            String[] args
    ) {

        try {

            BankApiServer server =
                    new BankApiServer();

            server.start();

        } catch (IOException e) {

            LOGGER.log(
                    Level.SEVERE,
                    "Could not start BankApiServer",
                    e
            );
        }
    }
}
