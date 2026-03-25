#pragma once

#include <cstdint>
#include <deque>
#include <string>
#include <vector>
#include <winsock2.h>

class ConnectionManager {
public:
    ConnectionManager(const std::string& serverIp, int serverPort);
    ~ConnectionManager();

    // Sends one UDP request and waits for a reply, retrying if needed.
    bool sendAndReceive(
        const std::vector<std::uint8_t>& message,
        std::vector<std::uint8_t>& response,
        int maxAttempts = 1
    );
    
    // Waits for a callback-style monitor update on the same socket.
    bool waitForMonitor(std::vector<std::uint8_t>& response); 
    bool setReceiveTimeout(int timeoutMilliseconds);

private:
    std::string serverIp;
    int serverPort;
    SOCKET clientSocket;
    sockaddr_in serverAddr;
    std::deque<std::vector<std::uint8_t>> pendingMonitorUpdates;

    void initializeWinsock();
    void cleanupWinsock();
    void createSocket();
    void configureServerAddress();
    void bindSocket();
    void configureDefaultTimeout();

    // Sends the already-marshalled request bytes to the configured server endpoint.
    bool sendRequest(const std::vector<std::uint8_t>& message);
    // Receives one full UDP datagram reply into a byte buffer.
    bool receiveResponse(std::vector<std::uint8_t>& response);
    bool receiveMatchingReply(std::uint32_t expectedRequestId, std::vector<std::uint8_t>& response);
};
