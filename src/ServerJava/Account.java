package ServerJava;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Account {
    private final int accountNumber;
    private final String holderName;
    private final String password;
    private final Currency currency;
    private double balance;
    private final List<String> history;

    public Account(int accountNumber, String holderName, String password, Currency currency, double balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.password = password;
        this.currency = currency;
        this.balance = balance;
        this.history = new ArrayList<>();
        record(String.format("Account opened with balance %.2f %s", balance, currency));
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getPassword() {
        return password;
    }

    public Currency getCurrency() {
        return currency;
    }

    public double getBalance() {
        return balance;
    }

    public boolean matchesOwner(String name, String candidatePassword) {
        return holderName.equals(name) && password.equals(candidatePassword);
    }

    public boolean passwordMatches(String candidatePassword) {
        return password.equals(candidatePassword);
    }

    public void deposit(double amount, String detail) {
        balance += amount;
        record(String.format("%s. Balance is now %.2f %s", detail, balance, currency.name()));
    }

    public void withdraw(double amount, String detail) {
        balance -= amount;
        record(String.format("%s. Balance is now %.2f %s", detail, balance, currency.name()));
    }

    public void record(String detail) {
        history.add(detail);
    }

    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
