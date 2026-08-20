package com.bank.exception;

public class AuthenticationException extends Exception {

    public AuthenticationException(String reason) {
        super("Authentication failed: " + reason);
    }

    public AuthenticationException(String message,Throwable cause) {
        super(message, cause);
    }
}