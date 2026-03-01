package Server;

import java.net.InetAddress;

public class BankRequest {
    private final int requestId;
    private final InetAddress clientAddress;
    private final int clientPort;
    private boolean isProcessed;
    private boolean isSuccessful;
    private byte[] responseMsg;

    private OperationRequest operationRequest;

    public BankRequest(int requestId, InetAddress clientAddress, int clientPort, OperationRequest operationRequest) {
        this.requestId = requestId;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.operationRequest = operationRequest;
        this.isProcessed = false;
        this.isSuccessful = false;
    }

    public int getRequestId() {
        return requestId;
    }

    public InetAddress getClientAddress() {
        return clientAddress;
    }

    public int getClientPort() {
        return clientPort;
    }

    public boolean isProcessed() {
        return isProcessed;
    }

    public void setProcessed(boolean processed) {
        isProcessed = processed;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void setSuccessful(boolean successful) {
        isSuccessful = successful;
    }

    public byte[] getResponseMsg() {
        return responseMsg;
    }

    public void setResponseMsg(byte[] responseMsg) {
        this.responseMsg = responseMsg;
    }

    public OperationRequest getOperationRequest() {
        return operationRequest;
    }

    public void setOperationRequest(OperationRequest operationRequest) {
        this.operationRequest = operationRequest;
    }
}
