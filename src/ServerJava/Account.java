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

    // Creates a new account and records its opening balance in the history log.
    public Account(int accountNumber, String holderName, String password, Currency currency, double balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.password = password;
        this.currency = currency;
        this.balance = balance;
        this.history = new ArrayList<>();
        record(String.format("Account opened with balance %.2f %s", balance, currency));
    }

    // Returns the unique account number assigned by the server.
    public int getAccountNumber() {
        return accountNumber;
    }

    // Returns the stored account holder name.
    public String getHolderName() {
        return holderName;
    }

    // Returns the stored account password for server-side checks.
    public String getPassword() {
        return password;
    }

    // Returns the currency used by this account.
    public Currency getCurrency() {
        return currency;
    }

    // Returns the account's current balance.
    public double getBalance() {
        return balance;
    }

    // Verifies that both the supplied owner name and password match this account.
    public boolean matchesOwner(String name, String candidatePassword) {
        return holderName.equals(name) && password.equals(candidatePassword);
    }

    // Verifies that the supplied password matches this account.
    public boolean passwordMatches(String candidatePassword) {
        return password.equals(candidatePassword);
    }

    // Adds funds to the account and records the updated balance in the history log.
    public void deposit(double amount, String detail) {
        balance += amount;
        record(String.format("%s. Balance is now %.2f %s", detail, balance, currency.name()));
    }

    // Deducts funds from the account and records the updated balance in the history log.
    public void withdraw(double amount, String detail) {
        balance -= amount;
        record(String.format("%s. Balance is now %.2f %s", detail, balance, currency.name()));
    }

    // Appends a new transaction entry to the account history.
    public void record(String detail) {
        history.add(detail);
    }

    // Returns a read-only view of the recorded transaction history.
    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
