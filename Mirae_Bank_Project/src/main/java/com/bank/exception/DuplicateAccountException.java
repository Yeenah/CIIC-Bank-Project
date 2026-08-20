package com.bank.exception;

public class DuplicateAccountException extends Exception {

    public DuplicateAccountException(String accountNumber) {
        super("An account already exists with account number: " + accountNumber);
    }

    public DuplicateAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}











