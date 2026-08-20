package com.bank.dao.implementation;

import com.bank.config.DBConnection;
import com.bank.dao.TransactionDAO;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionDAOImpl implements TransactionDAO {

    private static final Logger LOGGER = Logger.getLogger(TransactionDAOImpl.class.getName());
    private static final String SQL_INSERT =
            "INSERT INTO transactions " +
                    "(account_number, transaction_type, amount, balance_after, reference_number, remarks) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ACCOUNT =
            "SELECT transaction_id, account_number, transaction_type, amount, balance_after, " +
                    "       reference_number, remarks, created_at " +
                    "FROM transactions " +
                    "WHERE account_number = ? " +
                    "ORDER BY created_at DESC";

    private static final String SQL_FIND_RECENT =
            "SELECT transaction_id, account_number, transaction_type, amount, balance_after, " +
                    "       reference_number, remarks, created_at " +
                    "FROM transactions " +
                    "WHERE account_number = ? " +
                    "ORDER BY created_at DESC " +
                    "LIMIT ?";

    @Override
    public void save(Transaction transaction) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            save(conn, transaction);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save transaction: " + transaction.getReferenceNumber(), e);
            throw e;
        }
    }

    @Override
    public void save(Connection conn, Transaction transaction) throws SQLException {
        try (PreparedStatement preparedStatement = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, transaction.getAccountNumber());
            preparedStatement.setString(2, transaction.getTransactionType().name());
            preparedStatement.setBigDecimal(3, transaction.getAmount());
            preparedStatement.setBigDecimal(4, transaction.getBalanceAfter());
            preparedStatement.setString(5, transaction.getReferenceNumber());
            preparedStatement.setString(6, transaction.getRemarks());
            preparedStatement.executeUpdate();
            try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                if (keys.next()) {
                    transaction.setTransactionId(keys.getLong(1));
                }
            }

            LOGGER.fine("Transaction saved: " + transaction.getReferenceNumber());
        }
    }

    @Override
    public List<Transaction> findByAccountNumber(String accountNumber) throws SQLException {
        List<Transaction> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement prepareStatement = conn.prepareStatement(SQL_FIND_BY_ACCOUNT)) {

            prepareStatement.setString(1, accountNumber);

            try (ResultSet resultSet = prepareStatement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapRow(resultSet));
                }
            }
            return results;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch transactions for: " + accountNumber, e);
            throw e;
        }
    }

    @Override
    public List<Transaction> findRecentTransactions(String accountNumber, int limit)
            throws SQLException {

        List<Transaction> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_RECENT)) {

            ps.setString(1, accountNumber);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to fetch recent transactions for: " + accountNumber, e);
            throw e;
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction txn = new Transaction();
        txn.setTransactionId(rs.getLong("transaction_id"));
        txn.setAccountNumber(rs.getString("account_number"));
        txn.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        txn.setAmount(rs.getBigDecimal("amount"));
        txn.setBalanceAfter(rs.getBigDecimal("balance_after"));
        txn.setReferenceNumber(rs.getString("reference_number"));
        txn.setRemarks(rs.getString("remarks"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            txn.setCreatedAt(createdAt.toLocalDateTime());
        }

        return txn;
    }
}
