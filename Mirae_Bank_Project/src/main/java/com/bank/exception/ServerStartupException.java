package com.bank.exception;

public class ServerStartupException extends Exception {

    public ServerStartupException(String reason) {
        super("Mirae Bank API server failed to start: " + reason);
    }

    public ServerStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}