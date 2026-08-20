package com.bank.model;

public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER_OUT,
    TRANSFER_IN;

    public String displayTransType() {
        return name().replace('_', ' ');
    }

}
