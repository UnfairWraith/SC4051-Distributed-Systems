package ServerJava;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Random;

public final class BankServer {
    private static final int DEFAULT_PORT = 2222;
    private static final int MAX_PACKET_SIZE = 4096;

    private final DatagramSocket socket;
    private final InvocationMode invocationMode;
    private final double requestLossRate;
    private final double replyLossRate;
    private final Random random = new Random();
    private final Map<Integer, Account> accounts = new HashMap<>();
    private final List<MonitorRegistration> monitors = new ArrayList<>();
    private final Map<String, Protocol.Reply> replyHistory = new HashMap<>();
    private int nextAccountNumber = 1001;

    public BankServer(int port, InvocationMode invocationMode, double requestLossRate, double replyLossRate)
            throws Exception {
        this.socket = new DatagramSocket(port);
        this.invocationMode = invocationMode;
        this.requestLossRate = requestLossRate;
        this.replyLossRate = replyLossRate;
    }

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        InvocationMode invocationMode = InvocationMode.AT_MOST_ONCE;
        double requestLossRate = 0.0;
        double replyLossRate = 0.0;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            invocationMode = InvocationMode.fromArgument(args[1]);
        }
        if (args.length >= 3) {
            requestLossRate = parseLossRate(args[2], "request");
        }
        if (args.length >= 4) {
            replyLossRate = parseLossRate(args[3], "reply");
        }

        BankServer server = new BankServer(port, invocationMode, requestLossRate, replyLossRate);
        System.out.println("Java UDP bank server listening on port " + port
                + " using " + invocationMode.label + " semantics"
                + " (requestLossRate=" + requestLossRate
                + ", replyLossRate=" + replyLossRate + ")");
        server.run();
    }

    public void run() throws Exception {
        while (true) {
            byte[] buffer = new byte[MAX_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            SocketAddress clientAddress = packet.getSocketAddress();
            System.out.println("Received " + packet.getLength() + " bytes from " + clientAddress);

            if (shouldDrop(requestLossRate)) {
                System.out.println("Simulated request loss for packet from " + clientAddress);
                continue;
            }

            Protocol.Reply reply;
            try {
                reply = handleRequest(packet.getData(), packet.getLength(), clientAddress);
            } catch (Exception ex) {
                reply = new Protocol.Reply(0, Protocol.STATUS_ERROR,
                        "The server could not understand the request: " + ex.getMessage());
            }

            sendReply(clientAddress, reply);
        }
    }

    private Protocol.Reply handleRequest(byte[] data, int length, SocketAddress clientAddress) throws Exception {
        Protocol.Reader reader = new Protocol.Reader(data, length);
        int requestId = reader.readInt();
        int operation = reader.readInt();
        String requestKey = buildRequestKey(clientAddress, requestId);

        if (invocationMode == InvocationMode.AT_MOST_ONCE && replyHistory.containsKey(requestKey)) {
            return replyHistory.get(requestKey);
        }

        Protocol.Reply reply = switch (operation) {
            case Protocol.OP_OPEN -> handleOpen(requestId, reader);
            case Protocol.OP_CLOSE -> handleClose(requestId, reader);
            case Protocol.OP_DEPOSIT -> handleDeposit(requestId, reader);
            case Protocol.OP_WITHDRAW -> handleWithdraw(requestId, reader);
            case Protocol.OP_MONITOR -> handleMonitor(requestId, reader, clientAddress);
            case Protocol.OP_HISTORY -> handleHistory(requestId, reader);
            case Protocol.OP_TRANSFER -> handleTransfer(requestId, reader);
            default -> new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The requested operation is not supported.");
        };

        if (reader.hasRemaining()) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The request format is invalid because it contains unexpected extra data.");
        }

        if (invocationMode == InvocationMode.AT_MOST_ONCE) {
            replyHistory.put(requestKey, reply);
        }

        return reply;
    }

    private String buildRequestKey(SocketAddress clientAddress, int requestId) {
        return clientAddress.toString() + "#" + requestId;
    }

    private Protocol.Reply handleOpen(int requestId, Protocol.Reader reader) {
        String name = reader.readString();
        String password = reader.readString();
        Currency currency = parseCurrency(reader.readString());
        double initialBalance = reader.readDouble();

        if (initialBalance < 0) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "Initial balance must be zero or greater.");
        }
        if (currency == null) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The currency type is not valid.");
        }

        int accountNumber = nextAccountNumber++;
        Account account = new Account(accountNumber, name, password, currency, initialBalance);
        accounts.put(accountNumber, account);

        String message = "Account created successfully. Account number: " + accountNumber
                + ". Current balance: " + formatMoney(initialBalance) + " " + currency.name() + ".";
        notifyMonitors(message);
        return new Protocol.Reply(requestId, Protocol.STATUS_SUCCESS, message);
    }

    private Protocol.Reply handleClose(int requestId, Protocol.Reader reader) {
        String name = reader.readString();
        int accountNumber = reader.readInt();
        String password = reader.readString();

        Account account = accounts.get(accountNumber);
        if (account == null) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The specified account number does not exist.");
        }
        if (!account.matchesOwner(name, password)) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The name, account number, and password do not match our records.");
        }

        accounts.remove(accountNumber);
        String message = "Account " + accountNumber + " has been closed successfully.";
        notifyMonitors(message);
        return new Protocol.Reply(requestId, Protocol.STATUS_SUCCESS, message);
    }

    private Protocol.Reply handleDeposit(int requestId, Protocol.Reader reader) {
        String name = reader.readString();
        int accountNumber = reader.readInt();
        String password = reader.readString();
        Currency currency = parseCurrency(reader.readString());
        double amount = reader.readDouble();

        Account account = validateOwnedAccount(accountNumber, name, password);
        if (account == null) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The name, account number, and password do not match our records.");
        }
        if (currency == null) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The currency type is not valid.");
        }
        if (account.getCurrency() != currency) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The input currency does not match the account currency.");
        }
        if (amount <= 0) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "Deposit amount must be greater than zero.");
        }

        account.deposit(amount, "Deposited " + formatMoney(amount) + " " + currency.name());
        String message = "Deposit completed successfully. New balance for account " + accountNumber + ": "
                + formatMoney(account.getBalance()) + " " + currency.name() + ".";
        notifyMonitors("Deposit on account " + accountNumber + ": " + message);
        return new Protocol.Reply(requestId, Protocol.STATUS_SUCCESS, message);
    }

    private Protocol.Reply handleWithdraw(int requestId, Protocol.Reader reader) {
        String name = reader.readString();
        int accountNumber = reader.readInt();
        String password = reader.readString();
        Currency currency = parseCurrency(reader.readString());
        double amount = reader.readDouble();

        Account account = validateOwnedAccount(accountNumber, name, password);
        if (account == null) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The name, account number, and password do not match our records.");
        }
        if (currency == null) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The currency type is not valid.");
        }
        if (account.getCurrency() != currency) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The input currency does not match the account currency.");
        }
        if (amount <= 0) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "Withdrawal amount must be greater than zero.");
        }
        if (account.getBalance() < amount) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The account does not have enough balance for this withdrawal.");
        }

        account.withdraw(amount, "Withdrew " + formatMoney(amount) + " " + currency.name());
        String message = "Withdrawal completed successfully. New balance for account " + accountNumber + ": "
                + formatMoney(account.getBalance()) + " " + currency.name() + ".";
        notifyMonitors("Withdrawal on account " + accountNumber + ": " + message);
        return new Protocol.Reply(requestId, Protocol.STATUS_SUCCESS, message);
    }

    private Protocol.Reply handleMonitor(int requestId, Protocol.Reader reader, SocketAddress clientAddress) {
        int intervalSeconds = reader.readInt();
        if (intervalSeconds <= 0) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "Monitor interval must be greater than zero.");
        }

        Instant expiresAt = Instant.now().plusSeconds(intervalSeconds);
        monitors.removeIf(registration -> registration.clientAddress.equals(clientAddress));
        monitors.add(new MonitorRegistration(clientAddress, expiresAt));
        String message = "Monitoring has started for " + intervalSeconds + " seconds.";
        return new Protocol.Reply(requestId, Protocol.STATUS_SUCCESS, message);
    }

    private Protocol.Reply handleHistory(int requestId, Protocol.Reader reader) {
        int accountNumber = reader.readInt();
        String password = reader.readString();

        Account account = accounts.get(accountNumber);
        if (account == null) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The specified account number does not exist.");
        }
        if (!account.passwordMatches(password)) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The password for this account is incorrect.");
        }

        String message = String.join("\n", account.getHistory());
        if (message.isEmpty()) {
            message = "No transaction history is available for this account.";
        }

        return new Protocol.Reply(requestId, Protocol.STATUS_SUCCESS, message);
    }

    private Protocol.Reply handleTransfer(int requestId, Protocol.Reader reader) {
        int fromAccountNumber = reader.readInt();
        String password = reader.readString();
        int toAccountNumber = reader.readInt();
        Currency currency = parseCurrency(reader.readString());
        double amount = reader.readDouble();

        Account fromAccount = accounts.get(fromAccountNumber);
        Account toAccount = accounts.get(toAccountNumber);

        if (fromAccount == null || toAccount == null) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The source account or destination account does not exist.");
        }
        if (!fromAccount.passwordMatches(password)) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The password for the source account is incorrect.");
        }
        if (currency == null) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The currency code is not valid.");
        }
        if (fromAccount.getCurrency() != currency || toAccount.getCurrency() != currency) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "Both accounts must use the same currency as the transfer request.");
        }
        if (amount <= 0) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "Transfer amount must be greater than zero.");
        }
        if (fromAccount.getBalance() < amount) {
            return new Protocol.Reply(requestId, Protocol.STATUS_ERROR,
                    "The source account does not have enough balance for this transfer.");
        }

        fromAccount.withdraw(amount, "Transferred " + formatMoney(amount) + " " + currency.name()
                + " to account " + toAccountNumber);
        toAccount.deposit(amount, "Received " + formatMoney(amount) + " " + currency.name()
                + " from account " + fromAccountNumber);

        String message = "Transfer completed successfully.\n New balance for source account " + fromAccountNumber
                + ": " + formatMoney(fromAccount.getBalance()) + " " + currency.name()
                + ".\n New balance for destination account " + toAccountNumber + ": "
                + formatMoney(toAccount.getBalance()) + " " + currency.name() + ".";
        notifyMonitors("Transfer " + formatMoney(amount) + " " + currency.name() + " from account "
                + fromAccountNumber + " to account " + toAccountNumber);
        return new Protocol.Reply(requestId, Protocol.STATUS_SUCCESS, message);
    }

    private Account validateOwnedAccount(int accountNumber, String name, String password) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            return null;
        }
        return account.matchesOwner(name, password) ? account : null;
    }

    private void notifyMonitors(String updateMessage) {
        Instant now = Instant.now();
        Iterator<MonitorRegistration> iterator = monitors.iterator();

        while (iterator.hasNext()) {
            MonitorRegistration registration = iterator.next();
            if (!registration.expiresAt.isAfter(now)) {
                iterator.remove();
                continue;
            }

            Protocol.Reply update = new Protocol.Reply(0, Protocol.STATUS_UPDATE, updateMessage);
            try {
                sendReply(registration.clientAddress, update);
            } catch (Exception ex) {
                System.err.println("Failed to send monitor update: " + ex.getMessage());
            }
        }
    }

    private void sendReply(SocketAddress clientAddress, Protocol.Reply reply) throws Exception {
        if (shouldDrop(replyLossRate)) {
            System.out.println("Simulated reply loss for " + clientAddress);
            return;
        }

        byte[] payload = reply.toBytes();
        InetSocketAddress address = (InetSocketAddress) clientAddress;
        DatagramPacket packet = new DatagramPacket(
                payload,
                payload.length,
                InetAddress.getByAddress(address.getAddress().getAddress()),
                address.getPort());
        socket.send(packet);
        System.out.println("Sent reply to " + clientAddress + ": " + payload.length +
                " raw bytes");
    }

    private String formatMoney(double value) {
        return String.format("%.2f", value);
    }

    private boolean shouldDrop(double lossRate) {
        return lossRate > 0.0 && random.nextDouble() < lossRate;
    }

    private static double parseLossRate(String argument, String label) {
        double value = Double.parseDouble(argument);
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(label + " loss rate must be between 0.0 and 1.0");
        }
        return value;
    }

    private Currency parseCurrency(String currency) {
        try {
            return Currency.valueOf(currency.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static final class MonitorRegistration {
        private final SocketAddress clientAddress;
        private final Instant expiresAt;

        private MonitorRegistration(SocketAddress clientAddress, Instant expiresAt) {
            this.clientAddress = clientAddress;
            this.expiresAt = expiresAt;
        }
    }

    private enum InvocationMode {
        AT_LEAST_ONCE("at-least-once"),
        AT_MOST_ONCE("at-most-once");

        private final String label;

        InvocationMode(String label) {
            this.label = label;
        }

        private static InvocationMode fromArgument(String argument) {
            return "at-least-once".equals(argument) ? AT_LEAST_ONCE : AT_MOST_ONCE;
        }
    }
}
