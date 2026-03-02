#include <string>
#include <winsock2.h>

class ConnectionManager {
public:
    ConnectionManager(const std::string& serverIp, int serverPort);
    ~ConnectionManager();

    bool sendAndReceive(const std::string& message, std::string& response);
    
    bool waitForMonitor(std::string& response); 

private:
    std::string serverIp;
    int serverPort;
    SOCKET clientSocket;

    void initializeWinsock();
    void cleanupWinsock();
    void createSocket();
    void connectToServer();

    void sendRequest(const std::string& message);
    bool receiveResponse(std::string& response);
};