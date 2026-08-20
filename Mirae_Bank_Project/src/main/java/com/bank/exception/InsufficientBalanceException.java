package com.bank.exception;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class InsufficientBalanceException extends RuntimeException {

    private final BigDecimal availableBalance;
    private final BigDecimal requestAmount;

    public InsufficientBalanceException(BigDecimal availableBalance, BigDecimal requestAmount) {
        super(String.format(
                "Your available balance is insufficient to complete this transaction: %.2f, Request:%.2f",
                availableBalance, requestAmount
        ));
        this.availableBalance = availableBalance;
        this.requestAmount = requestAmount;
    }

}
