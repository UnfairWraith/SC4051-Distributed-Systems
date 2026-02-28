package Server;

import java.net.*;

public class Server {
    public static void main(String[] args) {
        printIPAddress();
        DatagramSocket aSocket = initializeSocket(6789);
        if (aSocket != null) {
            listenForRequests(aSocket);
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

    public static void listenForRequests(DatagramSocket socket) {
        byte[] buffer = new byte[1000];
        System.out.println("Server is running and waiting for client requests...");
        try (socket) {
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                handleRequest(request, socket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
