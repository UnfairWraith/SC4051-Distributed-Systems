#include "ConnectionManager.h"
#include "../Common/Protocol.h"

#include <cstdint>
#include <iomanip>
#include <iostream>
#include <limits>
#include <sstream>
#include <string>
#include <chrono>
#include <algorithm>
#include <cctype>
#include <vector>

namespace
{
    const int SERVER_PORT = 2222;
    const int MAX_RETRY_ATTEMPTS = 5;
    int reqId = 1;
    // The client validates currencies locally before sending the request.
    const std::vector<std::string> currencyList = {
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD",
        "INR", "SGD", "HKD", "ZAR", "MXN", "BRL", "KRW", "RUB", "NOK", "THB",
        "TRY", "DKK", "PLN", "TWD", "MYR", "IDR", "SAR", "AED", "ARS", "COP",
        "EGP", "CZK", "HUF", "ILS", "CLP", "PKR", "VND", "PHP", "KWD", "QAR",
        "MAD"};

    // Reads the server IP from the command line arguments.
    std::string parseServerIp(int argc, char *argv[])
    {
        if (argc < 2)
        {
            return {};
        }

        return argv[1];
    }

    // Displays a prompt label and returns the user's input line.
    std::string prompt(const std::string &label)
    {
        std::string value;
        std::cout << label;
        std::getline(std::cin, value);
        return value;
    }

    // Normalizes a currency code to uppercase before validation.
    std::string normalizeCurrency(std::string value)
    {
        // Match the server's validation by treating currency codes as uppercase values.
        std::transform(value.begin(), value.end(), value.begin(), [](unsigned char ch)
                       { return static_cast<char>(std::toupper(ch)); });
        return value;
    }

    // Checks whether the given currency is supported by the client.
    bool isValidCurrency(const std::string &currency)
    {
        return std::find(currencyList.begin(), currencyList.end(), currency) != currencyList.end();
    }

    // Re-prompts until the user enters a supported currency code.
    std::string promptCurrency(const std::string &label)
    {
        while (true)
        {
            const std::string currency = normalizeCurrency(prompt(label));
            if (isValidCurrency(currency))
            {
                return currency;
            }

            std::cout << "Please enter a valid currency.\n";
        }
    }

    // Re-prompts until the user enters a valid unsigned integer.
    std::uint32_t promptUint32(const std::string &label)
    {
        while (true)
        {
            const std::string value = prompt(label);
            std::istringstream input(value);
            std::uint32_t parsedValue = 0;

            if (input >> parsedValue && input.eof())
            {
                return parsedValue;
            }

            std::cout << "Please enter a valid non-negative integer.\n";
        }
    }

    // Re-prompts until the user enters a valid floating-point value.
    double promptDouble(const std::string &label)
    {
        while (true)
        {
            const std::string value = prompt(label);
            std::istringstream input(value);
            double parsedValue = 0.0;

            if (input >> parsedValue && input.eof())
            {
                return parsedValue;
            }

            std::cout << "Please enter a valid number.\n";
        }
    }

    // Creates the common request header for a new outbound operation.
    std::vector<std::uint8_t> createRequestHeader(protocol::Operation operation)
    {
        // Each request starts with a request id and operation code for duplicate detection.
        return protocol::createRequestHeader(reqId++, operation);
    }

    // Prints the list of available banking operations.
    void showMenu()
    {
        std::cout << "\nMenu:\n"
                  << "1. Open New Account\n"
                  << "2. Close Account\n"
                  << "3. Deposit\n"
                  << "4. Withdraw\n"
                  << "5. Monitor\n"
                  << "6. Transaction History\n"
                  << "7. Transfer Money\n"
                  << "8. Exit\n"
                  << "Choice: ";
    }

    // Builds the full request payload for the selected menu option.
    std::vector<std::uint8_t> buildRequest(int choice)
    {
        // Fields are appended in the exact order the Java server expects to unmarshal them.
        switch (choice)
        {
        case 1:
        {
            auto request = createRequestHeader(protocol::Operation::Open);
            protocol::appendString(request, prompt("Name: "));
            protocol::appendString(request, prompt("Password: "));
            const std::string currency = promptCurrency("Currency type (e.g. SGD, USD, EUR): ");
            protocol::appendString(request, currency);
            protocol::appendDouble(request, promptDouble("Initial balance: "));
            return request;
        }
        case 2:
        {
            auto request = createRequestHeader(protocol::Operation::Close);
            protocol::appendString(request, prompt("Name: "));
            protocol::appendUint32(request, promptUint32("Account number: "));
            protocol::appendString(request, prompt("Password: "));
            return request;
        }
        case 3:
        {
            auto request = createRequestHeader(protocol::Operation::Deposit);
            protocol::appendString(request, prompt("Name: "));
            protocol::appendUint32(request, promptUint32("Account number: "));
            protocol::appendString(request, prompt("Password: "));
            const std::string currency = promptCurrency("Currency type (e.g. SGD, USD, EUR): ");
            protocol::appendString(request, currency);
            protocol::appendDouble(request, promptDouble("Amount: "));
            return request;
        }
        case 4:
        {
            auto request = createRequestHeader(protocol::Operation::Withdraw);
            protocol::appendString(request, prompt("Name: "));
            protocol::appendUint32(request, promptUint32("Account number: "));
            protocol::appendString(request, prompt("Password: "));
            const std::string currency = promptCurrency("Currency type (e.g. SGD, USD, EUR): ");
            protocol::appendString(request, currency);
            protocol::appendDouble(request, promptDouble("Amount: "));
            return request;
        }
        case 5:
        {
            auto request = createRequestHeader(protocol::Operation::Monitor);
            protocol::appendUint32(request, promptUint32("Monitor interval in seconds: "));
            return request;
        }
        case 6:
        {
            auto request = createRequestHeader(protocol::Operation::History);
            protocol::appendUint32(request, promptUint32("Account number: "));
            protocol::appendString(request, prompt("Password: "));
            return request;
        }
        case 7:
        {
            auto request = createRequestHeader(protocol::Operation::Transfer);
            protocol::appendUint32(request, promptUint32("From account number: "));
            protocol::appendString(request, prompt("Password: "));
            protocol::appendUint32(request, promptUint32("To account number: "));
            const std::string currency = promptCurrency("Currency type (e.g. SGD, USD, EUR): ");
            protocol::appendString(request, currency);
            protocol::appendDouble(request, promptDouble("Amount: "));
            return request;
        }
        default:
            return {};
        }
    }

    // Extracts the monitor duration from an encoded monitor request.
    std::uint32_t extractMonitorIntervalSeconds(const std::vector<std::uint8_t> &request)
    {
        // Re-read the original monitor request so the client knows how long to block for updates.
        std::size_t offset = 0;
        std::uint32_t requestId = 0;
        std::uint32_t operation = 0;
        std::uint32_t intervalSeconds = 0;

        if (!protocol::readUint32(request, offset, requestId) ||
            !protocol::readUint32(request, offset, operation) ||
            !protocol::readUint32(request, offset, intervalSeconds) ||
            offset != request.size())
        {
            return 0;
        }

        if (operation != static_cast<std::uint32_t>(protocol::Operation::Monitor))
        {
            return 0;
        }

        return intervalSeconds;
    }

    // Converts a reply status enum into a readable label.
    std::string replyStatusToString(protocol::ReplyStatus status)
    {
        switch (status)
        {
        case protocol::ReplyStatus::Success:
            return "SUCCESS";
        case protocol::ReplyStatus::Error:
            return "ERROR";
        case protocol::ReplyStatus::Update:
            return "UPDATE";
        default:
            return "UNKNOWN";
        }
    }

    // Formats a byte buffer as hexadecimal text for debugging output.
    std::string formatBytes(const std::vector<std::uint8_t> &buffer)
    {
        // Hex output is useful when checking the marshalled packet format by hand.
        std::ostringstream output;
        output << std::hex << std::setfill('0');

        for (std::size_t i = 0; i < buffer.size(); ++i)
        {
            output << std::setw(2) << static_cast<int>(buffer[i]);
            if (i + 1 != buffer.size())
            {
                output << ' ';
            }
        }

        return output.str();
    }

    // Prints the user-facing message from a decoded server reply.
    void printReply(const protocol::Reply &reply)
    {
        // std::cout << "Reply requestId=" << reply.requestId
        //           << " status=" << replyStatusToString(reply.status)
        //           << " message=\"" << reply.message << "\"\n";
        // Keep the console focused on the server's user-facing message only.
        std::cout << reply.message << '\n';
    }

    // Waits for and prints monitor updates until the interval expires.
    void runMonitorLoop(ConnectionManager &connectionManager, std::uint32_t intervalSeconds)
    {
        if (!connectionManager.setReceiveTimeout(1000))
        {
            std::cout << "Unable to configure monitor receive timeout.\n";
            return;
        }

        // The lab allows the monitoring client to wait passively until the interval ends.
        std::cout << "Monitoring for " << intervalSeconds << " seconds. Waiting for server updates...\n";
        const auto deadline = std::chrono::steady_clock::now() + std::chrono::seconds(intervalSeconds);

        auto printMonitorResponse = [&](const std::vector<std::uint8_t> &response)
        {
            protocol::Reply reply;
            if (protocol::deserializeReply(response, reply))
            {
                printReply(reply);
            }
            else
            {
                std::cout << "Received non-protocol update (" << response.size()
                          << " bytes): " << formatBytes(response) << '\n';
            }
        };

        while (true)
        {
            const auto now = std::chrono::steady_clock::now();
            if (now >= deadline)
            {
                break;
            }

            const auto remaining = std::chrono::duration_cast<std::chrono::milliseconds>(deadline - now);
            const int timeoutMs = static_cast<int>(std::max<std::int64_t>(1, std::min<std::int64_t>(1000, remaining.count())));
            if (!connectionManager.setReceiveTimeout(timeoutMs))
            {
                std::cout << "Unable to configure monitor receive timeout.\n";
                break;
            }

            std::vector<std::uint8_t> response;
            if (!connectionManager.waitForMonitor(response))
            {
                continue;
            }

            printMonitorResponse(response);
        }

        // Drain any monitor updates that arrived just before the interval expired.
        if (connectionManager.setReceiveTimeout(50))
        {
            while (true)
            {
                std::vector<std::uint8_t> response;
                if (!connectionManager.waitForMonitor(response))
                {
                    break;
                }

                printMonitorResponse(response);
            }
        }

        connectionManager.setReceiveTimeout(5000);
        std::cout << "Monitor interval ended.\n";
    }
}

// Starts the client, sends user requests, and displays server responses.
int main(int argc, char *argv[])
{
    const std::string serverIp = parseServerIp(argc, argv);
    if (serverIp.empty())
    {
        std::cerr << "Usage: client.exe <server-ip>\n";
        return 1;
    }

    std::cout << "Client started. Sending UDP requests to " << serverIp << ":" << SERVER_PORT
              << " with up to " << MAX_RETRY_ATTEMPTS << " attempts per request.\n";

    ConnectionManager connectionManager(serverIp, SERVER_PORT);

    while (true)
    {
        showMenu();

        int choice = 0;
        if (!(std::cin >> choice))
        {
            std::cerr << "Invalid menu choice.\n";
            return 1;
        }

        std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');

        if (choice == 8)
        {
            std::cout << "Client is closing.\n";
            break;
        }

        std::vector<std::uint8_t> request = buildRequest(choice);
        if (request.empty())
        {
            std::cout << "Please choose a valid option from 1 to 8.\n";
            continue;
        }

        // Print the raw request bytes to make marshalling easier to inspect during testing.
        std::cout << "Sending " << request.size() << " bytes: " << formatBytes(request) << '\n';

        std::vector<std::uint8_t> response;
        if (connectionManager.sendAndReceive(request, response, MAX_RETRY_ATTEMPTS))
        {
            protocol::Reply reply;
            if (protocol::deserializeReply(response, reply))
            {
                printReply(reply);
                if (choice == 5 && reply.status == protocol::ReplyStatus::Success)
                {
                    const std::uint32_t intervalSeconds = extractMonitorIntervalSeconds(request);
                    if (intervalSeconds > 0)
                    {
                        runMonitorLoop(connectionManager, intervalSeconds);
                    }
                }
            }
            else
            {
                std::cout << "Received non-protocol reply (" << response.size()
                          << " bytes): " << formatBytes(response) << '\n';
            }
        }
        else
        {
            std::cout << "No response received from server.\n";
        }
    }

    return 0;
}
