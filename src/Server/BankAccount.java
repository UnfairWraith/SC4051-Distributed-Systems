package Server;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;

public class BankAccount {
    private final int accountNumber;
    private String name;
    private String password;
    private Currency currencyType;
    private int balance;

    public BankAccount(String name, String password, Currency currencyType, int balance) {
        this.accountNumber = Objects.hash(name, password, System.currentTimeMillis());
        this.name = name;
        this.password = hashPassword(password);
        this.currencyType = currencyType;
        this.balance = balance;
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
        return this.password.equals(hashPassword(password)); // Hopefully this works
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

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
