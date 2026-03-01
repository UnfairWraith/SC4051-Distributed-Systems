package Server;

import java.net.InetAddress;
import java.util.ArrayList;

public class RequestHistory {
    private ArrayList<OperationRequest> requestHistory;

    public RequestHistory() {
        this.requestHistory = new ArrayList<>();
    }

    public void addRequest(OperationRequest request) {
        requestHistory.add(request);
    }

    public OperationRequest findRequestById(int requestId, InetAddress clientAddress, int clientPort) {
        for (OperationRequest request : requestHistory) {
            if (request.getRequestId() == requestId && 
                request.getClientAddress().equals(clientAddress) && 
                request.getClientPort() == clientPort) {
                // Found the request, return it
                return request;
            }
        }
        // Request not found, handle accordingly
        return null;
    }

    public boolean isInHistory(int requestId, InetAddress clientAddress, int clientPort) {
        return findRequestById(requestId, clientAddress, clientPort) != null; // if found, return true
    }
}
