package Server;

public class OperationRequest {
    private Operation operationType;//operation type of the request
    private int requestId;//requestId of the request
    private int monitorTimeInterval;//monitor time interval for monitor operations
    private String clientAddress;//client address for monitor operations
    private int clientPort;//client port for monitor operations
    private int amount;//amount involved in the operation
    private String account;//account involved in the operation
    private String targetAccount;//target account for transfer operations
    private boolean isProcessed;
    private byte[] responseMsg;

    public OperationRequest(Operation operationType, int requestId, 
            int monitorTimeInterval, String clientAddress, int clientPort,
            int amount, String account, String targetAccount) {
        this.operationType = operationType;
        this.requestId = requestId;
        this.monitorTimeInterval = monitorTimeInterval;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.amount = amount;
        this.account = account;
        this.targetAccount = targetAccount;
        isProcessed = false;
        responseMsg = null;
    }

    // Getters and setters

    public Operation getOperationType() {
        return operationType;
    }

    public void setOperationType(Operation operationType) {
        this.operationType = operationType;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(String targetAccount) {
        this.targetAccount = targetAccount;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getMonitorTimeInterval() {
        return monitorTimeInterval;
    }

    public void setMonitorTimeInterval(int monitorTimeInterval) {
        this.monitorTimeInterval = monitorTimeInterval;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public boolean isProcessed() {
        return isProcessed;
    }

    public void setProcessed(boolean isProcessed) {
        this.isProcessed = isProcessed;
    }

    public byte[] getResponseMsg() {
        return responseMsg;
    }

    public void setResponseMsg(byte[] responseMsg) {
        this.responseMsg = responseMsg;
    }
}
