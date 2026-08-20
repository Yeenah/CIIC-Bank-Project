package com.bank.exception;

public class InvalidAmountException extends Exception {

    public InvalidAmountException(String reason) {
        super("Invalid transaction amount: " + reason);
    }

    public InvalidAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}