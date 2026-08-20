package com.bank.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class Transaction {
    private Long transactionId;
    private String accountNumber;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String referenceNumber;
    private String remarks;
    private LocalDateTime createdAt;

    public Transaction(String accountNumber, TransactionType transactionType, BigDecimal amount, BigDecimal balanceAfter, String referenceNumber, String remarks) {
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceNumber = referenceNumber;
        this.remarks = remarks;
    }

    public Transaction() {

    }

    @Override
    public String toString() {
        return String.format(
                "Transaction{id = '%d', ref = '%s', account = '%s', type = %s, amount = %.2f, balanceAfter = %.2f}",
                transactionId, referenceNumber, accountNumber, transactionType, amount, balanceAfter

        );
    }
}
