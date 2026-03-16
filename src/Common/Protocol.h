#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace protocol {

enum class Operation : std::uint32_t {
    Open = 1,
    Close = 2,
    Deposit = 3,
    Withdraw = 4,
    Monitor = 5,
    History = 6,
    Transfer = 7
};

enum class ReplyStatus : std::uint32_t {
    Success = 0,
    Error = 1,
    Update = 2
};

struct Reply {
    std::uint32_t requestId = 0;
    ReplyStatus status = ReplyStatus::Error;
    std::string message;
};

void appendUint32(std::vector<std::uint8_t>& buffer, std::uint32_t value);
void appendDouble(std::vector<std::uint8_t>& buffer, double value);
void appendString(std::vector<std::uint8_t>& buffer, const std::string& value);

bool readUint32(const std::vector<std::uint8_t>& buffer, std::size_t& offset, std::uint32_t& value);
bool readDouble(const std::vector<std::uint8_t>& buffer, std::size_t& offset, double& value);
bool readString(const std::vector<std::uint8_t>& buffer, std::size_t& offset, std::string& value);

std::vector<std::uint8_t> createRequestHeader(std::uint32_t requestId, Operation operation);
std::vector<std::uint8_t> serializeReply(const Reply& reply);
bool deserializeReply(const std::vector<std::uint8_t>& buffer, Reply& reply);

}  // namespace protocol
