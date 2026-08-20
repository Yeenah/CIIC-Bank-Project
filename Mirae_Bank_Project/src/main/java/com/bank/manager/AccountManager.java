package com.bank.manager;

import com.bank.dao.AccountDAO;
import com.bank.exception.AccountNotFoundException;
import com.bank.model.Account;
import com.bank.util.ReferenceNumberGenerator;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccountManager {

    private static final Logger LOGGER = Logger.getLogger(AccountManager.class.getName());

    private final AccountDAO accountDAO;
    private final Scanner scanner;

    public AccountManager(AccountDAO accountDAO, Scanner scanner) {
        this.accountDAO = accountDAO;
        this.scanner = scanner;
    }

    public void createAccount() {
        System.out.println("\n------ CREATE NEW ACCOUNT ------");

        System.out.print("Enter account holder name: ");
        String name = scanner.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("[ERROR] Account holder name cannot be empty.");
            return;
        }

        BigDecimal initialBalance = promptAmount("Enter initial deposit amount: ");
        if (initialBalance == null) return;
        String accountNumber = generateAccountNumber();

        Account account = new Account(accountNumber, name, initialBalance);

        try {
            accountDAO.createAccount(account);
            System.out.println("\n[SUCCESS] Account created successfully!");
            printAccountSummary(account);
            LOGGER.info("New account created: " + accountNumber + " for " + name);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create account for: " + name, e);
            System.out.println("[ERROR] Failed to create account: " + e.getMessage());
        }
    }

    public void balanceInquiry() {
        System.out.println("\n------ BALANCE INQUIRY ------");

        System.out.print("Enter account number: ");
        String accountNumber = scanner.nextLine().trim();

        try {
            Account account = findAccountOrThrow(accountNumber);
            System.out.println("\n[SUCCESS] Balance Inquiry");
            printAccountSummary(account);
        } catch (AccountNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Balance inquiry failed for: " + accountNumber, e);
            System.out.println("[ERROR] Database error: " + e.getMessage());
        }
    }

    public void listAccounts() {
        System.out.println("\n------ ALL ACCOUNTS ------");

        try {
            List<Account> accounts = accountDAO.findAllAccounts();

            if (accounts.isEmpty()) {
                System.out.println("No accounts found.");
                return;
            }

            printAccountsTable(accounts);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to list accounts", e);
            System.out.println("[ERROR] Failed to retrieve accounts: " + e.getMessage());
        }
    }

    public Account findAccountOrThrow(String accountNumber)
            throws AccountNotFoundException, SQLException {

        Optional<Account> opt = accountDAO.findByAccountNumber(accountNumber);
        return opt.orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private BigDecimal promptAmount(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        try {
            BigDecimal amount = new BigDecimal(input);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                System.out.println("[ERROR] Amount cannot be negative.");
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid amount: " + input);
            return null;
        }
    }

    private String generateAccountNumber() {
        long seed = System.currentTimeMillis() % 1_000_000_000L;
        return String.format("ACC-%010d", seed);
    }

    private void printAccountSummary(Account account) {
        System.out.println("  Account Number : " + account.getAccountNumber());
        System.out.println("  Account Name   : " + account.getAccountName());
        System.out.printf("  Balance        : PHP %.2f%n", account.getBalance());
        if (account.getCreatedAt() != null) {
            System.out.println("  Created At     : " + account.getCreatedAt());
        }
    }

    private void printAccountsTable(List<Account> accounts) {
        String line = "+----+-----------------------+-------------------------+---------------+---------------------+";
        System.out.println(line);
        System.out.printf("| %-2s | %-21s | %-23s | %-13s | %-19s |%n",
                "No", "Account Number", "Account Name", "Balance (PHP)", "Created At");
        System.out.println(line);

        int i = 1;
        for (Account a : accounts) {
            System.out.printf("| %-2d | %-21s | %-23s | %13.2f | %-19s |%n",
                    i++,
                    a.getAccountNumber(),
                    a.getAccountName(),
                    a.getBalance(),
                    a.getCreatedAt() != null ? a.getCreatedAt().toString().replace("T", " ") : "—"
            );
        }
        System.out.println(line);
        System.out.println("  Total accounts: " + accounts.size());
    }
}
