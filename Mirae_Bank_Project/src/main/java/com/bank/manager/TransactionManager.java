package com.bank.manager;

import com.bank.manager.AccountManager;
import com.bank.config.DBConnection;
import com.bank.dao.AccountDAO;
import com.bank.dao.TransactionDAO;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.InsufficientBalanceException;
import com.bank.exception.InvalidTransactionException;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;
import com.bank.util.ReferenceNumberGenerator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionManager {

    private static final Logger LOGGER = Logger.getLogger(TransactionManager.class.getName());

    private static final int MINI_STATEMENT_LIMIT = 10;
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final AccountManager accountService;
    private final Scanner scanner;

    public TransactionManager(AccountDAO accountDAO, TransactionDAO transactionDAO,
                              AccountManager accountService, Scanner scanner) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.accountService = accountService;
        this.scanner = scanner;
    }

    public void deposit() {
        System.out.println("\n------ DEPOSIT ------");

        try {
            Account account = promptAccount();
            BigDecimal amount = promptPositiveAmount("Enter deposit amount: ");

            BigDecimal newBalance = account.getBalance().add(amount);
            accountDAO.updateBalance(account.getAccountNumber(), newBalance);

            Transaction txn = new Transaction(
                    account.getAccountNumber(),
                    TransactionType.DEPOSIT,
                    amount,
                    newBalance,
                    ReferenceNumberGenerator.generate(),
                    "Cash deposit"
            );
            transactionDAO.save(txn);

            System.out.println("\n[SUCCESS] Deposit successful!");
            printTransactionReceipt(txn, account.getAccountName());
            LOGGER.info(String.format("DEPOSIT  | %s | %.2f | Ref: %s",
                    account.getAccountNumber(), amount, txn.getReferenceNumber()));

        } catch (AccountNotFoundException | InvalidTransactionException e) {
            System.out.println("[ERROR] " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Deposit failed", e);
            System.out.println("[ERROR] Deposit failed: " + e.getMessage());
        }
    }

    public void withdraw() {
        System.out.println("\n------ WITHDRAWAL ------");

        try {
            Account account = promptAccount();
            BigDecimal amount = promptPositiveAmount("Enter withdrawal amount: ");

            validateSufficientBalance(account, amount);

            BigDecimal newBalance = account.getBalance().subtract(amount);
            accountDAO.updateBalance(account.getAccountNumber(), newBalance);

            Transaction txn = new Transaction(
                    account.getAccountNumber(),
                    TransactionType.WITHDRAW,
                    amount,
                    newBalance,
                    ReferenceNumberGenerator.generate(),
                    "Cash withdrawal"
            );
            transactionDAO.save(txn);

            System.out.println("\n[SUCCESS] Withdrawal successful!");
            printTransactionReceipt(txn, account.getAccountName());
            LOGGER.info(String.format("WITHDRAW | %s | %.2f | Ref: %s",
                    account.getAccountNumber(), amount, txn.getReferenceNumber()));

        } catch (AccountNotFoundException | InvalidTransactionException |
                 InsufficientBalanceException e) {
            System.out.println("[ERROR] " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Withdrawal failed", e);
            System.out.println("[ERROR] Withdrawal failed: " + e.getMessage());
        }
    }

    public void transfer() {
        System.out.println("\n------ FUND TRANSFER ------");

        String senderNumber;
        String receiverNumber;
        BigDecimal amount;
        Account sender;
        Account receiver;
        try {
            System.out.print("Enter sender account number   : ");
            senderNumber = scanner.nextLine().trim();

            System.out.print("Enter receiver account number : ");
            receiverNumber = scanner.nextLine().trim();

            if (senderNumber.equalsIgnoreCase(receiverNumber)) {
                throw new InvalidTransactionException(
                        "Sender and receiver cannot be the same account."
                );
            }

            amount = promptPositiveAmount("Enter transfer amount         : ");
            sender = accountService.findAccountOrThrow(senderNumber);
            receiver = accountService.findAccountOrThrow(receiverNumber);

            validateSufficientBalance(sender, amount);

        } catch (AccountNotFoundException | InvalidTransactionException |
                 InsufficientBalanceException e) {
            System.out.println("[ERROR] " + e.getMessage());
            return;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Pre-transfer lookup failed", e);
            System.out.println("[ERROR] " + e.getMessage());
            return;
        }
        BigDecimal senderNewBalance = sender.getBalance().subtract(amount);
        BigDecimal receiverNewBalance = receiver.getBalance().add(amount);
        String sharedRef = ReferenceNumberGenerator.generate();

        Transaction transferOut = new Transaction(
                senderNumber,
                TransactionType.TRANSFER_OUT,
                amount,
                senderNewBalance,
                sharedRef,
                "Transfer to " + receiverNumber + " (" + receiver.getAccountName() + ")"
        );

        Transaction transferIn = new Transaction(
                receiverNumber,
                TransactionType.TRANSFER_IN,
                amount,
                receiverNewBalance,
                ReferenceNumberGenerator.generate(),   // distinct reference for receiver's ledger
                "Transfer from " + senderNumber + " (" + sender.getAccountName() + ")"
        );
        try (Connection conn = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                accountDAO.updateBalance(conn, senderNumber, senderNewBalance);
                accountDAO.updateBalance(conn, receiverNumber, receiverNewBalance);
                transactionDAO.save(conn, transferOut);
                transactionDAO.save(conn, transferIn);

                conn.commit();

                System.out.println("\n[SUCCESS] Fund transfer completed!");
                printTransactionReceipt(transferOut, sender.getAccountName());

                LOGGER.info(String.format(
                        "TRANSFER | %s → %s | %.2f | Ref: %s",
                        senderNumber, receiverNumber, amount, sharedRef
                ));

            } catch (SQLException e) {
                try {
                    conn.rollback();
                    LOGGER.warning("Transfer rolled back for ref: " + sharedRef);
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Rollback also failed!", rollbackEx);
                }
                LOGGER.log(Level.SEVERE, "Transfer failed, rolled back: " + sharedRef, e);
                System.out.println("[ERROR] Transfer failed and was rolled back: " + e.getMessage());
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not obtain connection for transfer", e);
            System.out.println("[ERROR] Could not connect to the database: " + e.getMessage());
        }
    }

    public void viewTransactionHistory() {
        System.out.println("\n------ TRANSACTION HISTORY ------");

        try {
            System.out.print("Enter account number: ");
            String accountNumber = scanner.nextLine().trim();

            accountService.findAccountOrThrow(accountNumber);   // validates account exists

            List<Transaction> transactions = transactionDAO.findByAccountNumber(accountNumber);

            if (transactions.isEmpty()) {
                System.out.println("No transactions found for account: " + accountNumber);
                return;
            }

            System.out.printf("%nTransaction History — %s%n", accountNumber);
            printTransactionsTable(transactions);

        } catch (AccountNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch transaction history", e);
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    public void miniStatement() {
        System.out.println("\n------ MINI STATEMENT (Last " + MINI_STATEMENT_LIMIT + " Transactions) ------");

        try {
            System.out.print("Enter account number: ");
            String accountNumber = scanner.nextLine().trim();

            Account account = accountService.findAccountOrThrow(accountNumber);

            List<Transaction> transactions =
                    transactionDAO.findRecentTransactions(accountNumber, MINI_STATEMENT_LIMIT);

            System.out.println("\n  Account : " + account.getAccountNumber());
            System.out.println("  Name    : " + account.getAccountName());
            System.out.printf("  Balance : PHP %.2f%n%n", account.getBalance());

            if (transactions.isEmpty()) {
                System.out.println("  No transactions found.");
                return;
            }

            printTransactionsTable(transactions);

        } catch (AccountNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch mini statement", e);
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private Account promptAccount() throws AccountNotFoundException, SQLException {
        System.out.print("Enter account number: ");
        String accountNumber = scanner.nextLine().trim();
        return accountService.findAccountOrThrow(accountNumber);
    }

    private BigDecimal promptPositiveAmount(String prompt) throws InvalidTransactionException {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        try {
            BigDecimal amount = new BigDecimal(input);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidTransactionException("Amount must be greater than zero.");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new InvalidTransactionException("Invalid amount: '" + input + "'");
        }
    }

    private void validateSufficientBalance(Account account, BigDecimal amount)
            throws InsufficientBalanceException {

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(account.getBalance(), amount);
        }
    }

    private void printTransactionReceipt(Transaction txn, String accountName) {
        System.out.println("  ----------------------------------");
        System.out.println("  Account       : " + txn.getAccountNumber());
        System.out.println("  Account Name  : " + accountName);
        System.out.println("  Type          : " + txn.getTransactionType().displayTransType());
        System.out.printf("  Amount        : PHP %.2f%n", txn.getAmount());
        System.out.printf("  Balance After : PHP %.2f%n", txn.getBalanceAfter());
        System.out.println("  Reference     : " + txn.getReferenceNumber());
        if (txn.getRemarks() != null && !txn.getRemarks().isEmpty()) {
            System.out.println("  Remarks       : " + txn.getRemarks());
        }
        System.out.println("  ----------------------------------");
    }

    private void printTransactionsTable(List<Transaction> transactions) {
        String line = "+----+---------------------+--------------+--------------+--------------+---------------------+";
        System.out.println(line);
        System.out.printf("| %-2s | %-19s | %-12s | %-12s | %-12s | %-19s |%n",
                "No", "Reference", "Type", "Amount", "Balance Aftr", "Date/Time");
        System.out.println(line);

        int i = 1;
        for (Transaction t : transactions) {
            System.out.printf("| %-2d | %-19s | %-12s | %12.2f | %12.2f | %-19s |%n",
                    i++,
                    t.getReferenceNumber().length() > 19
                            ? t.getReferenceNumber().substring(0, 19) : t.getReferenceNumber(),
                    t.getTransactionType().displayTransType(),
                    t.getAmount(),
                    t.getBalanceAfter(),
                    t.getCreatedAt() != null ? t.getCreatedAt().format(DISPLAY_FMT) : "—"
            );
        }
        System.out.println(line);
    }
}
