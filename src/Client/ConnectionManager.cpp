#include <ConnectionManager.h>
#include <iostream>

ConnectionManager::ConnectionManager(const std::string& serverIp, int serverPort) 
    : serverIp(serverIp), serverPort(serverPort), clientSocket(INVALID_SOCKET) {
    initializeWinsock();
    createSocket();
    connectToServer();
}

ConnectionManager::~ConnectionManager() {
    if (clientSocket != INVALID_SOCKET) {
        closesocket(clientSocket);
    }
    cleanupWinsock();
}

bool ConnectionManager::sendAndReceive(const std::string& message, std::string& response) {
    sendRequest(message);
    return receiveResponse(response);
}

bool ConnectionManager::waitForMonitor(std::string& response) {
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
    clientSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (clientSocket == INVALID_SOCKET) {
        std::cerr << "Error creating socket: " << WSAGetLastError() << std::endl;
        cleanupWinsock();
        exit(EXIT_FAILURE);
    }
}

void ConnectionManager::connectToServer() {
    sockaddr_in serverAddr;
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(serverPort);
    inet_pton(AF_INET, serverIp.c_str(), &serverAddr.sin_addr);

    if (connect(clientSocket, (sockaddr*)&serverAddr, sizeof(serverAddr)) == SOCKET_ERROR) {
        std::cerr << "Error connecting to server: " << WSAGetLastError() << std::endl;
        closesocket(clientSocket);
        cleanupWinsock();
        exit(EXIT_FAILURE);
    }
}

void ConnectionManager::sendRequest(const std::string& message) {
    int result = send(clientSocket, message.c_str(), static_cast<int>(message.size()), 0);
    if (result == SOCKET_ERROR) {
        std::cerr << "Error sending message: " << WSAGetLastError() << std::endl;
    }
}

bool ConnectionManager::receiveResponse(std::string& response) {
    char buffer[1024];
    int result = recv(clientSocket, buffer, sizeof(buffer) - 1, 0);
    if (result > 0) {
        buffer[result] = '\0'; // Null-terminate the received data
        response = std::string(buffer);
    } else if (result == 0) {
        std::cerr << "Connection closed by server." << std::endl;
    } else {
        std::cerr << "Error receiving message: " << WSAGetLastError() << std::endl;
    }
}