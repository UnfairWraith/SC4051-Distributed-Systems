package Server;

import java.util.Objects;

public class BankAccount {
    private final int accountNumber;
    private String name;
    private String hashedPassword;
    private Currency currencyType;
    private int balance;
    private RequestHistory transactionHistory;

    public BankAccount(String name, String password, Currency currencyType, int balance) {
        this.accountNumber = Objects.hash(name, password, System.currentTimeMillis());
        this.name = name;
        this.hashedPassword = password; // In a real app, this would be a hashed password
        this.currencyType = currencyType;
        this.balance = balance;
        this.transactionHistory = new RequestHistory();
    }

    // Overloaded constructor to allow currency type as a string
    public BankAccount(String name, String password, String currencyType, int balance) {
        this(name, password, Currency.valueOf(currencyType.trim().toUpperCase()), balance);
    }

    // Getters and setters

    public int getAccountNumber() {
        return accountNumber;
    }

    public String getName() {
        return name;
    }

    public boolean verifyAccountNumber(int accountNumber) {
        return this.accountNumber == accountNumber;
    }

    public boolean verifyName(String name) {
        return this.name.equals(name);
    }

    public boolean verifyPassword(String password) {
        return this.hashedPassword.equals(password);
    }
  
    public int getBalance() { 
        return balance; 
    }

    public Currency getCurrencyType() {
        return currencyType;
    } 

    public void deposit(int amount, Currency currency) {
        if (amount > 0 && currency == this.currencyType) {
            balance += amount;
        }
    }

    public boolean withdraw(int amount, Currency currency) {
        if (amount > 0 && balance >= amount && currency == this.currencyType) {
            balance -= amount;
            return true;
        }
        return false;
    }

    public RequestHistory getTransactionHistory() {
        return this.transactionHistory;
    }

    public void appendTransactionHistory(OperationRequest request) {
        this.transactionHistory.addRequest(request);
    }
}
