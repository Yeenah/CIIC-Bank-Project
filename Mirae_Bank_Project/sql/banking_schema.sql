--Mirae Bank Schema

-- Create database
CREATE DATABASE banking_db;
USE banking_db;


-- ==========================
-- ACCOUNTS TABLE
-- ==========================
CREATE TABLE accounts (
    account_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    account_name VARCHAR(100) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
      ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (account_id),

    INDEX idx_account_number (account_number),
    INDEX idx_created_at (created_at),

    CONSTRAINT chk_account_balance
      CHECK (balance >= 0)
) ENGINE=InnoDB;



-- ==========================
-- USERS TABLE
-- ==========================
CREATE TABLE users (
    user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    account_id BIGINT UNSIGNED NOT NULL,

    username VARCHAR(50) NOT NULL UNIQUE,

    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,

    active TINYINT(1) NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,


    PRIMARY KEY (user_id),

    INDEX idx_username (username),
    INDEX idx_account_id (account_id),


    CONSTRAINT fk_users_account
       FOREIGN KEY (account_id)
           REFERENCES accounts(account_id)
           ON UPDATE CASCADE
           ON DELETE RESTRICT

) ENGINE=InnoDB;



-- ==========================
-- TRANSACTIONS TABLE
-- ==========================
CREATE TABLE transactions (

    transaction_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    account_id BIGINT UNSIGNED NOT NULL,


    transaction_type ENUM(
    'DEPOSIT',
    'WITHDRAW',
    'TRANSFER_IN',
    'TRANSFER_OUT'
    ) NOT NULL,


    amount DECIMAL(15,2) NOT NULL,

    balance_after DECIMAL(15,2) NOT NULL,


    reference_number VARCHAR(30) NOT NULL UNIQUE,


    remarks VARCHAR(255) NULL,


    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,


    PRIMARY KEY(transaction_id),


    INDEX idx_account_id (account_id),

    INDEX idx_transaction_type(transaction_type),

    INDEX idx_created_at(created_at),

    INDEX idx_account_date(account_id, created_at),


    CONSTRAINT fk_transactions_account
      FOREIGN KEY(account_id)
          REFERENCES accounts(account_id)
          ON UPDATE CASCADE
          ON DELETE RESTRICT,


    CONSTRAINT chk_transaction_amount
      CHECK(amount > 0),


    CONSTRAINT chk_transaction_balance
      CHECK(balance_after >= 0)

) ENGINE=InnoDB;