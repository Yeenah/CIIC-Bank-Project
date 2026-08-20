package com.bank.exception;

public class InvalidTransactionException extends Exception {

    public InvalidTransactionException(String reason) {
        super("Invalid transaction: " + reason);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}





