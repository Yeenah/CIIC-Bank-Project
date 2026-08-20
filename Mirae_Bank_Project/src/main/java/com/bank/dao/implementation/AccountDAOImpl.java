package com.bank.dao.implementation;

import com.bank.config.DBConnection;
import com.bank.dao.AccountDAO;
import com.bank.model.Account;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccountDAOImpl implements AccountDAO {

    private static final Logger LOGGER = Logger.getLogger(AccountDAOImpl.class.getName());
    private static final String SQL_INSERT =
            "INSERT INTO accounts (account_number, account_name, balance) VALUES (?, ?, ?)";

    private static final String SQL_FIND_BY_NUMBER =
            "SELECT account_id, account_number, account_name, balance, created_at, updated_at " +
                    "FROM accounts WHERE account_number = ?";

    private static final String SQL_FIND_ALL =
            "SELECT account_id, account_number, account_name, balance, created_at, updated_at " +
                    "FROM accounts ORDER BY created_at ASC";

    private static final String SQL_UPDATE_BALANCE =
            "UPDATE accounts SET balance = ? WHERE account_number = ?";

    @Override
    public void createAccount(Account account) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, account.getAccountNumber());
            preparedStatement.setString(2, account.getAccountName());
            preparedStatement.setBigDecimal(3, account.getBalance());
            preparedStatement.executeUpdate();
            try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                if (keys.next()) {
                    account.setAccountId(keys.getLong(1));
                }
            }

            LOGGER.info("Account created: " + account.getAccountNumber());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create account: " + account.getAccountNumber(), e);
            throw e;
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_FIND_BY_NUMBER)) {

            preparedStatement.setString(1, accountNumber);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find account: " + accountNumber, e);
            throw e;
        }
    }

    @Override
    public List<Account> findAllAccounts() throws SQLException {
        List<Account> accounts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                accounts.add(mapRow(resultSet));
            }
            return accounts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to list all accounts", e);
            throw e;
        }
    }

    @Override
    public void updateBalance(String accountNumber, BigDecimal balance) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            updateBalance(conn, accountNumber, balance);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update balance for: " + accountNumber, e);
            throw e;
        }
    }

    @Override
    public void updateBalance(Connection conn, String accountNumber, BigDecimal balance)
            throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_BALANCE)) {
            ps.setBigDecimal(1, balance);
            ps.setString(2, accountNumber);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Balance update affected 0 rows for account: " + accountNumber);
            }

            LOGGER.fine("Balance updated for " + accountNumber + " → " + balance);
        }
    }

    private Account mapRow(ResultSet resultSet) throws SQLException {
        Account account = new Account();
        account.setAccountId(resultSet.getLong("account_id"));
        account.setAccountNumber(resultSet.getString("account_number"));
        account.setAccountName(resultSet.getString("account_name"));
        account.setBalance(resultSet.getBigDecimal("balance"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            account.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            account.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return account;
    }
}
