package Server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BankAccountRepo {
    private Map<Integer, BankAccount> accounts = new HashMap<>();

    public void addAccount(BankAccount account) {
        accounts.put(account.getAccountNumber(), account);
    }

    public BankAccount getAccount(int accountNumber) {
        return accounts.get(accountNumber);
    }

    public boolean accountExists(int accountNumber) {
        return accounts.containsKey(accountNumber);
    }

    public void removeAccount(int accountNumber) {
        accounts.remove(accountNumber);
    }

    public Collection<BankAccount> getAllAccounts() {
        return accounts.values();
    }
}
