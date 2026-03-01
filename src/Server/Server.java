package Server;

import java.net.*;

public class Server {
    private static RequestHistory requestHistory = new RequestHistory();
    private static double packetDrop = 0.3; // 30% packet drop rate


    public static void main(String[] args) {
        printIPAddress();
        requestHistory = new RequestHistory();
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

    // TODO - Implement handleRequest to process the request and generate appropriate responses based on the operation type
    public static void handleRequest(DatagramPacket request, DatagramSocket socket) {
        System.out.println("Received request: " + request);
        // Process the request and generate a response
        byte[] response = ("Response to: " + new String(request.getData(), 0, request.getLength())).getBytes();
        sendResponse(socket, request.getAddress(), request.getPort(), response);
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
