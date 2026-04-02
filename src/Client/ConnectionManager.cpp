#include "ConnectionManager.h"
#include "../Common/Protocol.h"

#include <iostream>
#include <ws2tcpip.h>

// Initializes the UDP client socket and stores the server endpoint.
ConnectionManager::ConnectionManager(const std::string& serverIp, int serverPort) 
    : serverIp(serverIp), serverPort(serverPort), clientSocket(INVALID_SOCKET) {
    // Prepare the UDP client socket and the known server endpoint once at startup.
    initializeWinsock();
    createSocket();
    configureServerAddress();
    bindSocket();
    configureDefaultTimeout();
}

// Releases the client socket and shuts down Winsock resources.
ConnectionManager::~ConnectionManager() {
    if (clientSocket != INVALID_SOCKET) {
        closesocket(clientSocket);
        clientSocket = INVALID_SOCKET;
    }
    cleanupWinsock();
}

// Sends one request and retries until a matching reply arrives or attempts run out.
bool ConnectionManager::sendAndReceive(
    const std::vector<std::uint8_t>& message,
    std::vector<std::uint8_t>& response,
    int maxAttempts
) {
    std::size_t offset = 0;
    std::uint32_t requestId = 0;
    if (!protocol::readUint32(message, offset, requestId)) {
        std::cerr << "Unable to read request id from outbound message." << std::endl;
        return false;
    }

    // Retries are used to demonstrate at-least-once and at-most-once semantics.
    for (int attempt = 1; attempt <= maxAttempts; ++attempt) {
        if (!sendRequest(message)) {
            return false;
        }

        if (receiveMatchingReply(requestId, response)) {
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

// Returns queued monitor updates first, then waits for a fresh datagram if needed.
bool ConnectionManager::waitForMonitor(std::vector<std::uint8_t>& response) {
    if (!pendingMonitorUpdates.empty()) {
        response = std::move(pendingMonitorUpdates.front());
        pendingMonitorUpdates.pop_front();
        return true;
    }

    return receiveResponse(response);
}

// Starts the Winsock library before any socket operations are used.
void ConnectionManager::initializeWinsock() {
    WSADATA wsaData;
    int result = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (result != 0) {
        std::cerr << "WSAStartup failed: " << result << std::endl;
        exit(EXIT_FAILURE);
    }
}

// Shuts down the Winsock library after client networking is finished.
void ConnectionManager::cleanupWinsock() {
    WSACleanup();
}

// Creates the UDP socket used for all client communication.
void ConnectionManager::createSocket() {
    // UDP communication uses a datagram socket instead of a TCP stream socket.
    clientSocket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (clientSocket == INVALID_SOCKET) {
        std::cerr << "Error creating socket: " << WSAGetLastError() << std::endl;
        cleanupWinsock();
        exit(EXIT_FAILURE);
    }
}

// Parses and stores the destination server IP address and port.
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

// Binds the client socket to an ephemeral local port chosen by the OS.
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

// Applies the default receive timeout used for normal request handling.
void ConnectionManager::configureDefaultTimeout() {
    setReceiveTimeout(5000);
}

// Updates the socket receive timeout so recvfrom can fail fast on loss.
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

// Sends one marshalled request datagram to the configured server.
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

// Receives a single UDP datagram and stores its raw bytes in the response buffer.
bool ConnectionManager::receiveResponse(std::vector<std::uint8_t>& response) {
    std::uint8_t buffer[4096];
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

// Waits until a non-monitor reply with the expected request id is received.
bool ConnectionManager::receiveMatchingReply(std::uint32_t expectedRequestId, std::vector<std::uint8_t>& response) {
    while (true) {
        std::vector<std::uint8_t> candidate;
        if (!receiveResponse(candidate)) {
            return false;
        }

        protocol::Reply reply;
        if (!protocol::deserializeReply(candidate, reply)) {
            response = std::move(candidate);
            return true;
        }

        if (reply.status == protocol::ReplyStatus::Update) {
            pendingMonitorUpdates.push_back(std::move(candidate));
            continue;
        }

        if (reply.requestId != expectedRequestId) {
            continue;
        }

        response = std::move(candidate);
        return true;
    }
}
