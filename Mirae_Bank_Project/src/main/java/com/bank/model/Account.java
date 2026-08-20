package com.bank.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
public class Account  {
    private Long accountId;
    private String accountNumber;
    private String accountName;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Account(String accountNumber, String accountName, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.balance = balance;
    }

    public Account() {

    }

    @Override
    public String toString() {
        return String.format(
                "Account{id=%s, number='%s', balance=%s}",
                accountId,
                accountNumber,
                balance
        );
    }

}
