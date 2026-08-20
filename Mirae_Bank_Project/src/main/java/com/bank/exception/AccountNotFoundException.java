package com.bank.exception;

public class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String accountNumber) {
        super("We couldn't find an account with the provided details: " + accountNumber);
    }
    public AccountNotFoundException(String message,Throwable cause) {super(message, cause);}
}
