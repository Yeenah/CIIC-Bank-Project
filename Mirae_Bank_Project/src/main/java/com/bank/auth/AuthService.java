package com.bank.auth;

import com.bank.dao.implementation.AccountDAOImpl;
import com.bank.exception.AuthenticationException;
import com.bank.exception.DuplicateAccountException;
import com.bank.exception.InvalidAccountException;
import com.bank.model.Account;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Logger;

public class AuthService {

    private static final Logger LOGGER =
            Logger.getLogger(AuthService.class.getName());

    private final AuthDAO authDAO =
            new AuthDAO();

    private final AccountDAOImpl accountDAO =
            new AccountDAOImpl();

    private static final SecureRandom RANDOM =
            new SecureRandom();


    public LoginResult login(
            String username,
            String password
    )
            throws SQLException, AuthenticationException {


        Optional<AuthDAO.UserCredentials> credentials =
                authDAO.findByUsername(username);


        if (credentials.isEmpty()) {

            LOGGER.warning(
                    "Login failed for username: " + username
            );

            throw new AuthenticationException(
                    "Invalid username or password."
            );
        }


        AuthDAO.UserCredentials user =
                credentials.get();


        if (!PasswordUtil.verifyPassword(
                password,
                user.passwordSalt(),
                user.passwordHash()
        )) {

            LOGGER.warning(
                    "Login failed for username: " + username
            );

            throw new AuthenticationException(
                    "Invalid username or password."
            );
        }


        Optional<Account> account =
                accountDAO.findByAccountNumber(
                        user.accountNumber()
                );


        if (account.isEmpty()) {

            LOGGER.warning(
                    "Login account not found: "
                            + username
            );

            throw new AuthenticationException(
                    "Account authentication failed."
            );
        }


        LOGGER.info(
                "Successful login: " + username
        );


        return new LoginResult(
                user.username(),
                account.get()
        );
    }



    public SignupResult signup(
            String fullName,
            String username,
            String email,
            String password
    )
            throws SQLException,
            InvalidAccountException,
            DuplicateAccountException {


        if (fullName == null ||
                fullName.isBlank()) {

            throw new InvalidAccountException(
                    "Full name is required."
            );
        }


        if (username == null ||
                username.isBlank()) {

            throw new InvalidAccountException(
                    "Username is required."
            );
        }


        if (email == null ||
                email.isBlank()) {

            throw new InvalidAccountException(
                    "Email is required."
            );
        }


        if (password == null ||
                password.length() < 6) {

            throw new InvalidAccountException(
                    "Password must be at least 6 characters."
            );
        }


        Optional<AuthDAO.UserCredentials> existingUser =
                authDAO.findByUsername(username);


        if (existingUser.isPresent()) {

            throw new DuplicateAccountException(
                    username
            );
        }


        String accountNumber =
                generateUniqueAccountNumber();


        Account account =
                new Account(
                        accountNumber,
                        fullName,
                        BigDecimal.ZERO
                );


        accountDAO.createAccount(account);


        String salt =
                PasswordUtil.generateSalt();


        String passwordHash =
                PasswordUtil.hashPassword(
                        password,
                        salt
                );


        authDAO.createUser(
                username,
                passwordHash,
                salt,
                accountNumber
        );


        LOGGER.info(
                "Signup successful: "
                        + username
                        + " -> "
                        + accountNumber
        );


        return new SignupResult(
                username,
                accountNumber,
                fullName
        );
    }



    private String generateUniqueAccountNumber()
            throws SQLException {


        for (int attempt = 0;
             attempt < 20;
             attempt++) {


            long number =
                    1_000_000_000L
                            + RANDOM.nextLong(
                            9_000_000_000L
                    );


            String accountNumber =
                    "ACC-" + number;


            Optional<Account> existing =
                    accountDAO.findByAccountNumber(
                            accountNumber
                    );


            if (existing.isEmpty()) {

                return accountNumber;
            }
        }


        throw new SQLException(
                "Unable to generate a unique account number."
        );
    }



    public record LoginResult(
            String username,
            Account account
    ) {}



    public record SignupResult(
            String username,
            String accountNumber,
            String fullName
    ) {}
}