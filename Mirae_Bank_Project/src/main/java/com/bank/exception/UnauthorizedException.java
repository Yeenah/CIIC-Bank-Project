package com.bank.exception;

public class UnauthorizedException extends Exception {

    public UnauthorizedException(String reason) {
        super("Unauthorized access: " + reason);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}