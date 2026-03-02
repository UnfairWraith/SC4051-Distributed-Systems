package Server;

public class BankService {
    private BankAccountRepo accountRepo;

    public BankService(BankAccountRepo accountRepo) {
        this.accountRepo = accountRepo;
    }

    public BankAccount openAccount(String name, String password, String currencyType, int initialDeposit) {
        BankAccount account = new BankAccount(name, password, currencyType, initialDeposit);
        accountRepo.addAccount(account);
        return account;
    }

    // Overloaded method to allow currency type as an enum
    public BankAccount openAccount(String name, String password, Currency currencyType, int initialDeposit) {
        BankAccount account = new BankAccount(name, password, currencyType, initialDeposit);
        accountRepo.addAccount(account);
        return account;
    }

    public void closeAccount(String name, int accountNumber, String password) {
        BankAccount account = accountRepo.getAccount(accountNumber);
        if (account != null && account.verifyName(name) && account.verifyPassword(password)) {
            if (account.getBalance() > 0) {
                System.out.println("Cannot close account with remaining balance.");
            } else {
                accountRepo.removeAccount(accountNumber);
                System.out.println("Account closed successfully.");
            }
        } else {
            System.out.println("Account verification failed. Please check your credentials.");
        }
    }

    public void deposit(String name, int accountNumber, String password, Currency currency, int amount) {
        BankAccount account = accountRepo.getAccount(accountNumber);
        if (account != null && account.verifyName(name) && account.verifyPassword(password)) {
            account.deposit(amount, currency);
            System.out.println("Deposit successful. New balance: " + account.getBalance());
        } else {
            System.out.println("Account verification failed. Please check your credentials.");
        }
    }

    public void processWithdrawal(String name, int accountNumber, String password, Currency currency, int amount) {
        BankAccount account = accountRepo.getAccount(accountNumber);
        if (account != null && account.verifyName(name) && account.verifyPassword(password)) {
            if (account.withdraw(amount, currency)) {
                System.out.println("Withdrawal successful. New balance: " + account.getBalance());
            } else {
                System.out.println("Withdrawal failed. Insufficient funds or currency mismatch.");
            }
        } else {
            System.out.println("Account verification failed. Please check your credentials.");
        }
    }

    public void monitor(int accountNumber, int timeInterval, String clientAddress, int clientPort) {
        // Implementation for monitoring an account
    }

    public RequestHistory viewTransactionHistory(String name, int accountNumber, String password) {
        // Implementation for viewing transaction history of an account
        BankAccount account = accountRepo.getAccount(accountNumber);
        // Verify account credentials
        if (account != null && account.verifyName(name) && account.verifyPassword(password)) {
            // Return transaction history for the account
            return account.getTransactionHistory();
        } else {
            System.out.println("Account verification failed. Please check your credentials.");
            return null;
        }
    }

    public void transfer(int sourceAccount, int targetAccount, int amount) {
        BankAccount source = accountRepo.getAccount(sourceAccount);
        BankAccount target = accountRepo.getAccount(targetAccount);
        if (source != null && target != null) {
            if (source.withdraw(amount, source.getCurrencyType())) {
                target.deposit(amount, target.getCurrencyType());
                System.out.println("Transfer successful. New balance of source account: " + source.getBalance());
            } else {
                System.out.println("Transfer failed. Insufficient funds in source account.");
            }
        } else {
            System.out.println("One or both accounts not found. Please check the account numbers.");
        }
    }
}
