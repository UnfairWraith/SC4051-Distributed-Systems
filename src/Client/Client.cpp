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
    const char *SERVER_IP = "127.0.0.1";
    const int SERVER_PORT = 2222;
    int reqId = 1;
    // The client validates currencies locally before sending the request.
    const std::vector<std::string> currencyList = {
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD",
        "INR", "SGD", "HKD", "ZAR", "MXN", "BRL", "KRW", "RUB", "NOK", "THB",
        "TRY", "DKK", "PLN", "TWD", "MYR", "IDR", "SAR", "AED", "ARS", "COP",
        "EGP", "CZK", "HUF", "ILS", "CLP", "PKR", "VND", "PHP", "KWD", "QAR",
        "MAD"};

    enum class InvocationMode
    {
        AtLeastOnce,
        AtMostOnce
    };

    InvocationMode parseInvocationMode(int argc, char *argv[])
    {
        if (argc < 2)
        {
            return InvocationMode::AtMostOnce;
        }

        const std::string mode = argv[1];
        if (mode == "at-least-once")
        {
            return InvocationMode::AtLeastOnce;
        }

        return InvocationMode::AtMostOnce;
    }

    std::string invocationModeToString(InvocationMode mode)
    {
        return mode == InvocationMode::AtLeastOnce ? "at-least-once" : "at-most-once";
    }

    int retryAttemptsForMode(InvocationMode mode)
    {
        return mode == InvocationMode::AtLeastOnce ? 3 : 3;
    }

    std::string prompt(const std::string &label)
    {
        std::string value;
        std::cout << label;
        std::getline(std::cin, value);
        return value;
    }

    std::string normalizeCurrency(std::string value)
    {
        // Match the server's validation by treating currency codes as uppercase values.
        std::transform(value.begin(), value.end(), value.begin(), [](unsigned char ch)
                       { return static_cast<char>(std::toupper(ch)); });
        return value;
    }

    bool isValidCurrency(const std::string &currency)
    {
        return std::find(currencyList.begin(), currencyList.end(), currency) != currencyList.end();
    }

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

    std::vector<std::uint8_t> createRequestHeader(protocol::Operation operation)
    {
        // Each request starts with a request id and operation code for duplicate detection.
        return protocol::createRequestHeader(reqId++, operation);
    }

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

    std::vector<std::uint8_t> buildRequest(int choice)
    {
        // Fields are appended in the exact order the Java server expects to unmarshal them.
        switch (choice)
        {
        case 1:
        {
            auto request = createRequestHeader(protocol::Operation::Open);
            const std::string currency = promptCurrency("Currency type (e.g. SGD, USD, EUR): ");
            protocol::appendString(request, prompt("Name: "));
            protocol::appendString(request, prompt("Password: "));
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
            const std::string currency = promptCurrency("Currency type (e.g. SGD, USD, EUR): ");
            protocol::appendString(request, prompt("Name: "));
            protocol::appendUint32(request, promptUint32("Account number: "));
            protocol::appendString(request, prompt("Password: "));
            protocol::appendString(request, currency);
            protocol::appendDouble(request, promptDouble("Amount: "));
            return request;
        }
        case 4:
        {
            auto request = createRequestHeader(protocol::Operation::Withdraw);
            const std::string currency = promptCurrency("Currency type (e.g. SGD, USD, EUR): ");
            protocol::appendString(request, prompt("Name: "));
            protocol::appendUint32(request, promptUint32("Account number: "));
            protocol::appendString(request, prompt("Password: "));
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
            const std::string currency = promptCurrency("Currency type (e.g. SGD, USD, EUR): ");
            protocol::appendUint32(request, promptUint32("From account number: "));
            protocol::appendString(request, prompt("Password: "));
            protocol::appendUint32(request, promptUint32("To account number: "));
            protocol::appendString(request, currency);
            protocol::appendDouble(request, promptDouble("Amount: "));
            return request;
        }
        default:
            return {};
        }
    }

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

    void printReply(const protocol::Reply &reply)
    {
        // std::cout << "Reply requestId=" << reply.requestId
        //           << " status=" << replyStatusToString(reply.status)
        //           << " message=\"" << reply.message << "\"\n";
        // Keep the console focused on the server's user-facing message only.
        std::cout << reply.message << '\n';
    }

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

        while (std::chrono::steady_clock::now() < deadline)
        {
            std::vector<std::uint8_t> response;
            if (!connectionManager.waitForMonitor(response))
            {
                continue;
            }

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
        }

        connectionManager.setReceiveTimeout(5000);
        std::cout << "Monitor interval ended.\n";
    }
}

int main(int argc, char *argv[])
{
    const InvocationMode invocationMode = parseInvocationMode(argc, argv);
    std::cout << "Client started. Sending UDP requests to " << SERVER_IP << ":" << SERVER_PORT
              << " using " << invocationModeToString(invocationMode) << " semantics.\n";

    ConnectionManager connectionManager(SERVER_IP, SERVER_PORT);

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
        if (connectionManager.sendAndReceive(request, response, retryAttemptsForMode(invocationMode)))
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
