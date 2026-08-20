package com.bank.service;

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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionService {

    private static final Logger LOGGER =
            Logger.getLogger(TransactionService.class.getName());

    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;

    public TransactionService(
            AccountDAO accountDAO,
            TransactionDAO transactionDAO
    ) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
    }

    public Transaction deposit(
            String accountNumber,
            BigDecimal amount
    ) throws SQLException,
            AccountNotFoundException,
            InvalidTransactionException {

        validateAccountNumber(accountNumber);
        validateAmount(amount);

        try (Connection conn = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                Account account =
                        findAccountForUpdate(
                                conn,
                                accountNumber
                        );

                BigDecimal newBalance =
                        account.getBalance()
                                .add(amount);
                accountDAO.updateBalance(
                        conn,
                        accountNumber,
                        newBalance
                );
                Transaction transaction =
                        new Transaction(
                                accountNumber,
                                TransactionType.DEPOSIT,
                                amount,
                                newBalance,
                                ReferenceNumberGenerator.generate(),
                                "Cash deposit"
                        );
                transactionDAO.save(
                        conn,
                        transaction
                );
                conn.commit();

                LOGGER.info(
                        "DEPOSIT | " +
                                accountNumber +
                                " | " +
                                amount +
                                " | Ref: " +
                                transaction.getReferenceNumber()
                );

                return transaction;

            } catch (AccountNotFoundException |
                     SQLException e) {

                rollback(conn);
                throw e;

            } catch (RuntimeException e) {

                rollback(conn);

                LOGGER.log(
                        Level.SEVERE,
                        "Unexpected deposit error.",
                        e
                );

                throw new SQLException(
                        "Deposit failed.",
                        e
                );

            } finally {

                resetAutoCommit(conn);
            }
        }
    }

    public Transaction withdraw(
            String accountNumber,
            BigDecimal amount
    ) throws SQLException,
            AccountNotFoundException,
            InvalidTransactionException,
            InsufficientBalanceException {

        validateAccountNumber(accountNumber);
        validateAmount(amount);

        try (Connection conn = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                Account account =
                        findAccountForUpdate(
                                conn,
                                accountNumber
                        );
                if (account.getBalance()
                        .compareTo(amount) < 0) {

                    throw new InsufficientBalanceException(
                            account.getBalance(),
                            amount
                    );
                }

                BigDecimal newBalance =
                        account.getBalance()
                                .subtract(amount);
                accountDAO.updateBalance(
                        conn,
                        accountNumber,
                        newBalance
                );
                Transaction transaction =
                        new Transaction(
                                accountNumber,
                                TransactionType.WITHDRAW,
                                amount,
                                newBalance,
                                ReferenceNumberGenerator.generate(),
                                "Cash withdrawal"
                        );
                transactionDAO.save(
                        conn,
                        transaction
                );
                conn.commit();

                LOGGER.info(
                        "WITHDRAW | " +
                                accountNumber +
                                " | " +
                                amount +
                                " | Ref: " +
                                transaction.getReferenceNumber()
                );

                return transaction;

            } catch (AccountNotFoundException |
                     InsufficientBalanceException |
                     SQLException e) {

                rollback(conn);
                throw e;

            } catch (RuntimeException e) {

                rollback(conn);

                LOGGER.log(
                        Level.SEVERE,
                        "Unexpected withdrawal error.",
                        e
                );

                throw new SQLException(
                        "Withdrawal failed.",
                        e
                );

            } finally {

                resetAutoCommit(conn);
            }
        }
    }

    public TransferResult transfer(
            String senderAccountNumber,
            String receiverAccountNumber,
            BigDecimal amount
    ) throws SQLException,
            AccountNotFoundException,
            InvalidTransactionException,
            InsufficientBalanceException {

        validateAccountNumber(senderAccountNumber);
        validateAccountNumber(receiverAccountNumber);
        validateAmount(amount);
        if (senderAccountNumber.equalsIgnoreCase(
                receiverAccountNumber
        )) {

            throw new InvalidTransactionException(
                    "Sender and receiver cannot be the same account."
            );
        }

        try (Connection conn =
                     DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                String firstAccount;
                String secondAccount;

                if (senderAccountNumber.compareTo(
                        receiverAccountNumber
                ) < 0) {

                    firstAccount =
                            senderAccountNumber;

                    secondAccount =
                            receiverAccountNumber;

                } else {

                    firstAccount =
                            receiverAccountNumber;

                    secondAccount =
                            senderAccountNumber;
                }
                Account first =
                        findAccountForUpdate(
                                conn,
                                firstAccount
                        );
                Account second =
                        findAccountForUpdate(
                                conn,
                                secondAccount
                        );
                Account sender;

                if (senderAccountNumber.equals(
                        first.getAccountNumber()
                )) {

                    sender = first;

                } else {

                    sender = second;
                }
                Account receiver;

                if (receiverAccountNumber.equals(
                        first.getAccountNumber()
                )) {

                    receiver = first;

                } else {

                    receiver = second;
                }
                if (sender.getBalance()
                        .compareTo(amount) < 0) {

                    throw new InsufficientBalanceException(
                            sender.getBalance(),
                            amount
                    );
                }
                BigDecimal senderNewBalance =
                        sender.getBalance()
                                .subtract(amount);

                BigDecimal receiverNewBalance =
                        receiver.getBalance()
                                .add(amount);

                String transferReference =
                        ReferenceNumberGenerator.generate();
                Transaction transferOut =
                        new Transaction(
                                senderAccountNumber,
                                TransactionType.TRANSFER_OUT,
                                amount,
                                senderNewBalance,
                                transferReference,
                                "Transfer to " +
                                        receiverAccountNumber +
                                        " (" +
                                        receiver.getAccountName() +
                                        ")"
                        );
                Transaction transferIn =
                        new Transaction(
                                receiverAccountNumber,
                                TransactionType.TRANSFER_IN,
                                amount,
                                receiverNewBalance,
                                ReferenceNumberGenerator.generate(),
                                "Transfer from " +
                                        senderAccountNumber +
                                        " (" +
                                        sender.getAccountName() +
                                        ")"
                        );

                accountDAO.updateBalance(
                        conn,
                        senderAccountNumber,
                        senderNewBalance
                );

                accountDAO.updateBalance(
                        conn,
                        receiverAccountNumber,
                        receiverNewBalance
                );

                transactionDAO.save(
                        conn,
                        transferOut
                );

                transactionDAO.save(
                        conn,
                        transferIn
                );

                conn.commit();

                LOGGER.info(
                        "TRANSFER | " +
                                senderAccountNumber +
                                " -> " +
                                receiverAccountNumber +
                                " | " +
                                amount +
                                " | Ref: " +
                                transferReference
                );

                return new TransferResult(
                        transferOut,
                        transferIn
                );

            } catch (AccountNotFoundException |
                     InsufficientBalanceException |
                     SQLException e) {

                rollback(conn);
                throw e;

            } catch (RuntimeException e) {

                rollback(conn);

                LOGGER.log(
                        Level.SEVERE,
                        "Unexpected transfer error.",
                        e
                );

                throw new SQLException(
                        "Transfer failed.",
                        e
                );

            } finally {

                resetAutoCommit(conn);
            }
        }
    }

    public List<Transaction> findByAccountNumber(
            String accountNumber
    ) throws SQLException,
            AccountNotFoundException,
            InvalidTransactionException {

        validateAccountNumber(accountNumber);

        Account account =
                accountDAO.findByAccountNumber(
                        accountNumber
                ).orElseThrow(
                        () -> new AccountNotFoundException(
                                accountNumber
                        )
                );

        return transactionDAO.findByAccountNumber(
                account.getAccountNumber()
        );
    }

    public List<Transaction> findRecentTransactions(
            String accountNumber,
            int limit
    ) throws SQLException,
            AccountNotFoundException,
            InvalidTransactionException {

        validateAccountNumber(accountNumber);

        if (limit <= 0) {
            limit = 10;
        }

        if (limit > 100) {
            limit = 100;
        }

        Account account =
                accountDAO.findByAccountNumber(
                        accountNumber
                ).orElseThrow(
                        () -> new AccountNotFoundException(
                                accountNumber
                        )
                );

        return transactionDAO.findRecentTransactions(
                account.getAccountNumber(),
                limit
        );
    }

    private Account findAccountForUpdate(
            Connection conn,
            String accountNumber
    ) throws SQLException,
            AccountNotFoundException {

        String sql =
                "SELECT account_id, account_number, account_name, " +
                        "balance, created_at, updated_at " +
                        "FROM accounts " +
                        "WHERE account_number = ? " +
                        "FOR UPDATE";

        try (PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(
                    1,
                    accountNumber
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (!rs.next()) {

                    throw new AccountNotFoundException(
                            accountNumber
                    );
                }

                Account account =
                        new Account();

                account.setAccountId(
                        rs.getLong("account_id")
                );

                account.setAccountNumber(
                        rs.getString("account_number")
                );

                account.setAccountName(
                        rs.getString("account_name")
                );

                account.setBalance(
                        rs.getBigDecimal("balance")
                );

                Timestamp createdAt =
                        rs.getTimestamp(
                                "created_at"
                        );

                if (createdAt != null) {

                    account.setCreatedAt(
                            createdAt.toLocalDateTime()
                    );
                }

                Timestamp updatedAt =
                        rs.getTimestamp(
                                "updated_at"
                        );

                if (updatedAt != null) {

                    account.setUpdatedAt(
                            updatedAt.toLocalDateTime()
                    );
                }

                return account;
            }
        }
    }

    private void validateAccountNumber(
            String accountNumber
    ) throws InvalidTransactionException {

        if (accountNumber == null ||
                accountNumber.trim().isEmpty()) {

            throw new InvalidTransactionException(
                    "Account number is required."
            );
        }
    }

    private void validateAmount(
            BigDecimal amount
    ) throws InvalidTransactionException {

        if (amount == null) {

            throw new InvalidTransactionException(
                    "Amount is required."
            );
        }

        if (amount.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new InvalidTransactionException(
                    "Amount must be greater than zero."
            );
        }

        if (amount.scale() > 2) {

            throw new InvalidTransactionException(
                    "Amount cannot have more than 2 decimal places."
            );
        }
    }

    private void rollback(
            Connection conn
    ) {

        try {

            conn.rollback();

        } catch (SQLException e) {

            LOGGER.log(
                    Level.SEVERE,
                    "Database rollback failed.",
                    e
            );
        }
    }

    private void resetAutoCommit(
            Connection conn
    ) {

        try {

            conn.setAutoCommit(true);

        } catch (SQLException ignored) {
        }
    }

    public record TransferResult(
            Transaction transferOut,
            Transaction transferIn
    ) {
    }
}
