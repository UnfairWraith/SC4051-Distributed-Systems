#include "ConnectionManager.h"
#include <iostream>
#include <ws2tcpip.h>

ConnectionManager::ConnectionManager(const std::string& serverIp, int serverPort) 
    : serverIp(serverIp), serverPort(serverPort), clientSocket(INVALID_SOCKET) {
    // Prepare the UDP client socket and the known server endpoint once at startup.
    initializeWinsock();
    createSocket();
    configureServerAddress();
    bindSocket();
    configureDefaultTimeout();
}

ConnectionManager::~ConnectionManager() {
    if (clientSocket != INVALID_SOCKET) {
        closesocket(clientSocket);
        clientSocket = INVALID_SOCKET;
    }
    cleanupWinsock();
}

bool ConnectionManager::sendAndReceive(
    const std::vector<std::uint8_t>& message,
    std::vector<std::uint8_t>& response,
    int maxAttempts
) {
    // Retries are used to demonstrate at-least-once and at-most-once semantics.
    for (int attempt = 1; attempt <= maxAttempts; ++attempt) {
        if (!sendRequest(message)) {
            return false;
        }

        if (receiveResponse(response)) {
            return true;
        }

        if (attempt < maxAttempts) {
            std::cout << "No response received for attempt " << attempt
                      << ". Retrying request (" << (attempt + 1)
                      << "/" << maxAttempts << ")...\n";
        }
    }

    return false;
}

bool ConnectionManager::waitForMonitor(std::vector<std::uint8_t>& response) {
    return receiveResponse(response);
}

void ConnectionManager::initializeWinsock() {
    WSADATA wsaData;
    int result = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (result != 0) {
        std::cerr << "WSAStartup failed: " << result << std::endl;
        exit(EXIT_FAILURE);
    }
}

void ConnectionManager::cleanupWinsock() {
    WSACleanup();
}

void ConnectionManager::createSocket() {
    // UDP communication uses a datagram socket instead of a TCP stream socket.
    clientSocket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (clientSocket == INVALID_SOCKET) {
        std::cerr << "Error creating socket: " << WSAGetLastError() << std::endl;
        cleanupWinsock();
        exit(EXIT_FAILURE);
    }
}

void ConnectionManager::configureServerAddress() {
    // Store the server IP and port once so sendto() can reuse them for every request.
    ZeroMemory(&serverAddr, sizeof(serverAddr));
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(serverPort);
    int result = inet_pton(AF_INET, serverIp.c_str(), &serverAddr.sin_addr);
    if (result != 1) {
        std::cerr << "Invalid server IP address: " << serverIp << std::endl;
        closesocket(clientSocket);
        cleanupWinsock();
        exit(EXIT_FAILURE);
    }
}

void ConnectionManager::bindSocket() {
    sockaddr_in localAddr;
    ZeroMemory(&localAddr, sizeof(localAddr));
    localAddr.sin_family = AF_INET;
    localAddr.sin_addr.s_addr = htonl(INADDR_ANY);
    // Port 0 lets the OS choose an available client port automatically.
    localAddr.sin_port = htons(0);

    if (bind(clientSocket, reinterpret_cast<sockaddr*>(&localAddr), sizeof(localAddr)) == SOCKET_ERROR) {
        std::cerr << "Error binding client socket: " << WSAGetLastError() << std::endl;
        closesocket(clientSocket);
        cleanupWinsock();
        exit(EXIT_FAILURE);
    }
}

void ConnectionManager::configureDefaultTimeout() {
    setReceiveTimeout(5000);
}

bool ConnectionManager::setReceiveTimeout(int timeoutMilliseconds) {
    // Socket-level timeouts let recvfrom() return when a reply is lost.
    DWORD timeout = static_cast<DWORD>(timeoutMilliseconds);
    int result = setsockopt(
        clientSocket,
        SOL_SOCKET,
        SO_RCVTIMEO,
        reinterpret_cast<const char*>(&timeout),
        sizeof(timeout)
    );

    if (result == SOCKET_ERROR) {
        std::cerr << "Error setting receive timeout: " << WSAGetLastError() << std::endl;
        return false;
    }

    return true;
}

bool ConnectionManager::sendRequest(const std::vector<std::uint8_t>& message) {
    // The client sends raw marshalled bytes; the server handles decoding.
    int result = sendto(
        clientSocket,
        reinterpret_cast<const char*>(message.data()),
        static_cast<int>(message.size()),
        0,
        reinterpret_cast<sockaddr*>(&serverAddr),
        sizeof(serverAddr)
    );

    if (result == SOCKET_ERROR) {
        std::cerr << "Error sending message: " << WSAGetLastError() << std::endl;
        return false;
    }

    return true;
}

bool ConnectionManager::receiveResponse(std::vector<std::uint8_t>& response) {
    std::uint8_t buffer[1024];
    sockaddr_in fromAddr;
    int fromAddrLength = sizeof(fromAddr);

    int result = recvfrom(
        clientSocket,
        reinterpret_cast<char*>(buffer),
        sizeof(buffer) - 1,
        0,
        reinterpret_cast<sockaddr*>(&fromAddr),
        &fromAddrLength
    );

    if (result > 0) {
        // Each recvfrom() call returns one complete UDP datagram.
        response.assign(buffer, buffer + result);
        return true;
    } else if (result == 0) {
        std::cerr << "Connection closed by server." << std::endl;
    } else {
        int error = WSAGetLastError();
        if (error != WSAETIMEDOUT) {
            std::cerr << "Error receiving message: " << error << std::endl;
        }
        // Timeouts are expected during retry experiments, so they are not logged as failures here.
    }

    return false;
}
