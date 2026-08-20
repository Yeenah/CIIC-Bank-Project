package com.bank.dao;

import com.bank.model.Account;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface AccountDAO {
    void createAccount(Account account) throws SQLException;

    Optional<Account> findByAccountNumber(String accountNumber) throws SQLException;

    List<Account> findAllAccounts() throws SQLException;

    void updateBalance(String accountNumber, BigDecimal balance) throws SQLException;

    void updateBalance(Connection conn, String accountNumber, BigDecimal balance) throws SQLException;
}
