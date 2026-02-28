package Server;

public class OperationRequest {
    private Operation operationType;//operation type of the request
    private int requestId;//request id of the request
    private String account;//account involved in the operation
    private String targetAccount;//target account for transfer operations (optional)
    private int clientPort;//client port
    private String clientAddress;//client address
    private long timestamp;//timestamp of the update operation
    
    public OperationRequest(Operation operationType, int requestId, String account, String targetAccount, int clientPort, String clientAddress, long timestamp) {
        this.operationType = operationType;
        this.requestId = requestId;
        this.account = account;
        this.targetAccount = targetAccount;
        this.clientPort = clientPort;
        this.clientAddress = clientAddress;
        this.timestamp = timestamp;
    }

    public Operation getOperationType() {
        return operationType;
    }

    public void setOperationType(Operation operationType) {
        this.operationType = operationType;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
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

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
