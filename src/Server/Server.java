package Server;

import java.net.*;

public class Server {
    private static RequestHistory requestHistory = new RequestHistory();
    private static double packetDrop = 0.3; // 30% packet drop rate
    private static String semantic = "at-least-once"; // Default to at-least-once semantics


    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("at-most-once") || args[0].equalsIgnoreCase("at-least-once")) {
                semantic = args[0].toLowerCase();
            } else {
                System.out.println("Unknow semantic, defaulting to at-least-once");
                semantic = "at-least-once";
            }
        }
        if (args.length > 1) {
            try {
                packetDrop = Double.parseDouble(args[1]);
                if (packetDrop < 0 || packetDrop > 1) {
                    System.out.println("Invalid packet drop rate, defaulting to 0.3");
                    packetDrop = 0.3;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid packet drop rate format, defaulting to " + packetDrop);
            }
        }
        printIPAddress();
        requestHistory = new RequestHistory();
        BankAccountRepo repo = new BankAccountRepo();
        PacketDrop packetDropSimulator = new PacketDrop(packetDrop);
        DatagramSocket aSocket = initializeSocket(6789);
        if (aSocket != null) {
            listenForRequests(aSocket, packetDropSimulator);
        }
    }

    public static void printIPAddress() {
        try {
            System.out.println("Banking System Server IP Address: " + java.net.InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static DatagramSocket initializeSocket(int port) {
        try {
            return new DatagramSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void listenForRequests(DatagramSocket socket, PacketDrop packetDropSimulator) {
        byte[] buffer = new byte[1000];
        System.out.println("Server is running and waiting for client requests...");
        try (socket) {
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                if (packetDropSimulator.shouldDrop()) {
                    System.out.println("Packet dropped from: " + request.getAddress() + ":" + request.getPort());
                    continue; // Skip processing this request
                } else {
                    System.out.println("Received request from: " + request.getAddress() + ":" + request.getPort());
                    handleRequest(request, socket);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleRequest(DatagramPacket request, DatagramSocket socket) {
        System.out.println("Received request: " + request);
        InetAddress clientAddress = request.getAddress();
        int clientPort = request.getPort();
        byte[] requestData = request.getData();

        OperationRequest opReq = Unmarshalling.unmarshalRequest(requestData, clientAddress, clientPort);
        if (opReq == null) {
            System.out.println("Failed to unmarshal request from: " + clientAddress + ":" + clientPort);
            return;
        }

        int requestId = opReq.getRequestId();
        byte[] response;

        if (semantic.equals("at-most-once")) {
            // Check for duplicate request using requestId, clientAddress, and clientPort
            OperationRequest existingEntry = requestHistory.findRequestById(requestId, clientAddress, clientPort);
            if (existingEntry != null && existingEntry.isProcessed()) {
                System.out.println("Duplicate request detected (at-most-once)");
                response = existingEntry.getResponseMsg();
                if (response != null) {
                    sendResponse(socket, clientAddress, clientPort, response);
                } else {
                    System.out.println("No response found for duplicate request");
                }
            } else { // Process the request and store the response in history
                response = processRequest(opReq);
                sendResponse(socket, clientAddress, clientPort, response);
                opReq.setProcessed(true);
                opReq.setResponseMsg(response);
                // add the request to history after processing
                requestHistory.addRequest(opReq);
            }
        } else if (semantic.equals("at-least-once")) { // Process the request without checking for duplicates
            response = processRequest(opReq);
            sendResponse(socket, clientAddress, clientPort, response);
            opReq.setProcessed(true);
            opReq.setResponseMsg(response);
            // add the request to history after processing
            requestHistory.addRequest(opReq);
            }
    }

    public static byte[] processRequest(OperationRequest opReq) {
        // TODO - Implement the logic to process the request based on the operation type and return the appropriate response
        byte[] response = "filler response".getBytes();
        switch(opReq.getOperationType()) {
            case OPEN_ACCT:
                // Handle open account operation
                break;
            case CLOSE_ACCT:
                // Handle close account operation
                break;
            case DEPOSIT:
                // Handle deposit operation
                break;
            case WITHDRAW:
                // Handle withdraw operation
                break;
            case MONITOR:
                // Handle monitor operation
                break;
            case VIEW_TX_HISTORY:
                // Handle view transaction history operation
                break;
            case TRANSFER:
                // Handle transfer operation
                break;
            default:
                // Handle unknown operation
                break;
        }
        return response;
    }

    public static void sendResponse(DatagramSocket socket, InetAddress address, int port, byte[] response) {
        System.out.println("Sending response: " + new String(response));
        try {
            DatagramPacket responsePacket = new DatagramPacket(response, response.length, address, port);
            socket.send(responsePacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
