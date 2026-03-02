#include <iostream>

#define SERVER_IP "127.0.0.1"
#define SERVER_PORT 2222

int reqId = 0;

int main() {
    std::cout << "Client started. Connecting to server at " << SERVER_IP << ":" << SERVER_PORT << "..." << std::endl;

    // Here you would typically create a socket, connect to the server, and send/receive data.
    // For this example, we'll just simulate a connection.

    std::cout << "Connected to server successfully!" << std::endl;

    // Simulate sending a message to the server
    std::string message = "Hello, Server!";
    std::cout << "Sending message to server: " << message << std::endl;

    // Simulate receiving a response from the server
    std::string response = "Hello, Client!";
    std::cout << "Received response from server: " << response << std::endl;

    std::cout << "Client is closing the connection." << std::endl;
    return 0;
}

void showMenu() {
    std::cout << "Menu:" << std::endl;
    std::cout << "1. Open New Account" << std::endl;
    std::cout << "2. Close Account" << std::endl;
    std::cout << "3. Deposit" << std::endl;
    std::cout << "4. Withdraw" << std::endl;
    std::cout << "5. Monitor" << std::endl;
    std::cout << "6. Transaction History" << std::endl;
    std::cout << "7. Transfer Money" << std::endl;
    std::cout << "8. Exit" << std::endl;
}