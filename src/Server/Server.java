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
        BankService bankService = new BankService(repo);
        PacketDrop packetDropSimulator = new PacketDrop(packetDrop);
        DatagramSocket aSocket = initializeSocket(6789);
        if (aSocket != null) {
            listenForRequests(aSocket, packetDropSimulator, bankService);
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

    public static void listenForRequests(DatagramSocket socket, PacketDrop packetDropSimulator, BankService bankService) {
        byte[] buffer = new byte[1000];
        System.out.println("Server is running and waiting for client requests...");
        try (socket) {
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                if (packetDropSimulator.shouldDrop()) {
                    System.out.println("Packet dropped from: " + request.getAddress() + ":" + request.getPort());
                } else {
                    System.out.println("Received request from: " + request.getAddress() + ":" + request.getPort());
                    handleRequest(request, socket, bankService);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleRequest(DatagramPacket request, DatagramSocket socket, BankService bankService) {
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
                response = processRequest(opReq, bankService);
                sendResponse(socket, clientAddress, clientPort, response);
                opReq.setProcessed();
                opReq.setResponseMsg(response);
                // add the request to history after processing
                requestHistory.addRequest(opReq);
            }
        } else if (semantic.equals("at-least-once")) { // Process the request without checking for duplicates
            response = processRequest(opReq, bankService);
            sendResponse(socket, clientAddress, clientPort, response);
            opReq.setProcessed();
            opReq.setResponseMsg(response);
            // add the request to history after processing
            requestHistory.addRequest(opReq);
            }
    }

    public static byte[] processRequest(OperationRequest opReq, BankService bankService) {
        byte[] response = "filler response".getBytes();
        switch(opReq.getOperationType()) {
            case OPEN_ACCT -> {
                // Handle open account operation
                BankAccount account = bankService.openAccount(opReq.getName(), opReq.getPassword(), opReq.getCurrencyType(), opReq.getAmount());
                response = ("Account opened successfully. Account Number: " + account.getAccountNumber()).getBytes();
            }
            case CLOSE_ACCT -> {
                // Handle close account operation
                bankService.closeAccount(opReq.getName(), opReq.getAccountNumber(), opReq.getPassword());
                response = "Account closed successfully.".getBytes();
            }
            case DEPOSIT -> {
                // Handle deposit operation
                bankService.deposit(opReq.getName(), opReq.getAccountNumber(), opReq.getPassword(), opReq.getCurrencyType(), opReq.getAmount());
                response = "Deposit successful.".getBytes();
            }
            case WITHDRAW -> {
                // Handle withdraw operation
                bankService.processWithdrawal(opReq.getName(), opReq.getAccountNumber(), opReq.getPassword(), opReq.getCurrencyType(), opReq.getAmount());
                response = "Withdrawal successful.".getBytes();
            }
            case MONITOR -> // Handle monitor operation
                // bankService.monitor(opReq.getAccountNumber(), opReq.getMonitorTimeInterval(), opReq.getClientAddress(), opReq.getClientPort());
                response = "Monitoring started.".getBytes();
            case VIEW_TX_HISTORY -> {
                // Handle view transaction history operation
                bankService.viewTransactionHistory(opReq.getName(), opReq.getAccountNumber(), opReq.getPassword());
                response = "Transaction history retrieved.".getBytes();
            }
            case TRANSFER -> {
                // Handle transfer operation
                bankService.transfer(opReq.getAccountNumber(), opReq.getTargetAccountNumber(), opReq.getAmount());
                response = "Transfer successful.".getBytes();
            }
            default -> {
                // Handle unknown operation
            }
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
