package com.bank.auth;

import com.bank.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AuthDAO {

    public record UserCredentials(
            String username,
            String passwordHash,
            String passwordSalt,
            String accountNumber
    ) {}

    private static final String SQL_FIND =
            "SELECT username, password_hash, password_salt, account_number " +
                    "FROM users " +
                    "WHERE username = ? AND active = TRUE";

    private static final String SQL_INSERT =
            "INSERT INTO users " +
                    "(username, password_hash, password_salt, account_number, active) " +
                    "VALUES (?, ?, ?, ?, TRUE)";

    public Optional<UserCredentials> findByUsername(
            String username
    ) throws SQLException {

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(SQL_FIND)
        ) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        new UserCredentials(
                                rs.getString("username"),
                                rs.getString("password_hash"),
                                rs.getString("password_salt"),
                                rs.getString("account_number")
                        )
                );
            }
        }
    }

    public void createUser(
            String username,
            String passwordHash,
            String passwordSalt,
            String accountNumber
    ) throws SQLException {

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(SQL_INSERT)
        ) {

            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, passwordSalt);
            ps.setString(4, accountNumber);

            ps.executeUpdate();
        }
    }
}
