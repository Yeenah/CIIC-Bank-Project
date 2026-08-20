package com.bank.menu;
import com.bank.dao.AccountDAO;
import com.bank.dao.TransactionDAO;
import com.bank.dao.implementation.AccountDAOImpl;
import com.bank.dao.implementation.TransactionDAOImpl;
import com.bank.manager.AccountManager;
import com.bank.manager.TransactionManager;

import java.util.Scanner;
import java.util.logging.Logger;

public class BankDashboard {

    private final AccountManager accountmanager;
    private final TransactionManager transactionmanager;
    private final Scanner scanner;

    public BankDashboard() {
        scanner = new Scanner(System.in);
        AccountDAO accountDAO = new AccountDAOImpl();
        TransactionDAO transactionDAO = new TransactionDAOImpl();

        accountmanager = new AccountManager(accountDAO, scanner);
        transactionmanager = new TransactionManager(accountDAO, transactionDAO, accountmanager, scanner);
    }

    public void openDashboard() {
        boolean isOpen = true;
        while (isOpen) {
            showDashboard();

            System.out.println("Please select your option: ");
            String input = scanner.nextLine();

            switch (input) {
                case "1" -> accountmanager.createAccount();
                case "2" -> accountmanager.balanceInquiry();
                case "3" -> transactionmanager.deposit();
                case "4" -> transactionmanager.withdraw();
                case "5" -> transactionmanager.transfer();
                case "6" -> transactionmanager.viewTransactionHistory();
                case "7" -> transactionmanager.miniStatement();
                case "8" -> accountmanager.listAccounts();
                case "9" -> {
                    isOpen = false;
                    System.out.println("\nThank you for using Mirae Bank, We care for your future");
                }
                default -> System.out.println("Invalid option " + input + "Must enter 1-9");
            }

        }

        scanner.close();
    }
    private void showDashboard() {
        System.out.println();
        System.out.println("==================================");
        System.out.println("    BANKING MANAGEMENT SYSTEM     ");
        System.out.println("==================================");
        System.out.println(" 1. Create Account");
        System.out.println(" 2. Balance Inquiry");
        System.out.println(" 3. Deposit");
        System.out.println(" 4. Withdraw");
        System.out.println(" 5. Transfer");
        System.out.println(" 6. Transaction History");
        System.out.println(" 7. Mini Statement");
        System.out.println(" 8. List All Accounts");
        System.out.println(" 9. Exit");
        System.out.println("==================================");

    }
}
