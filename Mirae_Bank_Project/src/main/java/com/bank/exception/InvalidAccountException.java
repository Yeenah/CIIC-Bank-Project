package com.bank.exception;

public class InvalidAccountException extends Exception {

    public InvalidAccountException(String reason) {
        super("Invalid account information: " + reason);
    }

    public InvalidAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}