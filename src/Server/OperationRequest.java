package Server;

import java.security.MessageDigest;
import java.util.Base64;

public class OperationRequest {
    private final Operation operationType;//operation type of the request
    private final int requestId;//requestId of the request
    private final String name;//name of the client for authentication
    private final int accountNumber;//account number for authentication
    private final String hashedPassword;//hashed password for authentication
    private final Currency currencyType;//currency type for deposit, withdraw, and transfer operations
    private final int amount;//amount involved in the operation
    private final int targetAccountNumber;//target account number for transfer operations
    private final int monitorTimeInterval;//monitor time interval for monitor operations
    private final String clientAddress;//client address for monitor operations
    private final int clientPort;//client port for monitor operations
    private boolean isProcessed;
    private byte[] responseMsg;

    public OperationRequest(Operation operationType, int requestId, 
            int monitorTimeInterval, String clientAddress, int clientPort,
            String name, int accountNumber, String password, Currency currencyType, int amount, int targetAccountNumber) {
        this.operationType = operationType;
        this.requestId = requestId;
        this.name = name;
        this.accountNumber = accountNumber;
        this.hashedPassword = hashPassword(password);
        this.currencyType = currencyType;
        this.amount = amount;
        this.targetAccountNumber = targetAccountNumber;
        this.monitorTimeInterval = monitorTimeInterval;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        isProcessed = false;
        responseMsg = null;
    }

    // Getters and setters

    public Operation getOperationType() {
        return operationType;
    }

    public int getRequestId() {
        return requestId;
    }

    public String getName() {
        return name;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public String getPassword() {
        return hashedPassword;
    }

    public Currency getCurrencyType() {
        return currencyType;
    }

    public int getAmount() {
        return amount;
    }

    public int getTargetAccountNumber() {
        return targetAccountNumber;
    }


    public int getMonitorTimeInterval() {
        return monitorTimeInterval;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public int getClientPort() {
        return clientPort;
    }

    public boolean isProcessed() {
        return isProcessed;
    }

    public void setProcessed() {
        this.isProcessed = true;
    }

    public byte[] getResponseMsg() {
        return responseMsg;
    }

    public void setResponseMsg(byte[] responseMsg) {
        this.responseMsg = responseMsg;
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
