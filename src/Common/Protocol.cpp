#include "Protocol.h"

#include <cstring>
#include <winsock2.h>

namespace protocol
{
    namespace
    {

        // Converts a 64-bit value from host order to network byte order.
        std::uint64_t hostToNetwork64(std::uint64_t value)
        {
            const std::uint32_t lowPart = htonl(static_cast<std::uint32_t>(value & 0xFFFFFFFFULL));
            const std::uint32_t highPart = htonl(static_cast<std::uint32_t>(value >> 32));
            return (static_cast<std::uint64_t>(lowPart) << 32) | highPart;
        }

        // Converts a 64-bit value from network order back to host byte order.
        std::uint64_t networkToHost64(std::uint64_t value)
        {
            const std::uint32_t lowPart = ntohl(static_cast<std::uint32_t>(value & 0xFFFFFFFFULL));
            const std::uint32_t highPart = ntohl(static_cast<std::uint32_t>(value >> 32));
            return (static_cast<std::uint64_t>(lowPart) << 32) | highPart;
        }

    } // namespace

    // Appends a 32-bit unsigned integer to the byte buffer in network order.
    void appendUint32(std::vector<std::uint8_t> &buffer, std::uint32_t value)
    {
        const std::uint32_t networkValue = htonl(value);
        const auto *bytes = reinterpret_cast<const std::uint8_t *>(&networkValue);
        buffer.insert(buffer.end(), bytes, bytes + sizeof(networkValue));
    }

    // Appends a double value to the byte buffer using a network-order bit pattern.
    void appendDouble(std::vector<std::uint8_t> &buffer, double value)
    {
        static_assert(sizeof(double) == sizeof(std::uint64_t), "Unexpected double size.");

        std::uint64_t rawValue = 0;
        std::memcpy(&rawValue, &value, sizeof(value));
        rawValue = hostToNetwork64(rawValue);

        const auto *bytes = reinterpret_cast<const std::uint8_t *>(&rawValue);
        buffer.insert(buffer.end(), bytes, bytes + sizeof(rawValue));
    }

    // Appends a length-prefixed UTF-8 string to the byte buffer.
    void appendString(std::vector<std::uint8_t> &buffer, const std::string &value)
    {
        appendUint32(buffer, static_cast<std::uint32_t>(value.size()));
        const auto *bytes = reinterpret_cast<const std::uint8_t *>(value.data());
        buffer.insert(buffer.end(), bytes, bytes + value.size());
    }

    // Reads a 32-bit unsigned integer from the buffer and advances the offset.
    bool readUint32(const std::vector<std::uint8_t> &buffer, std::size_t &offset, std::uint32_t &value)
    {
        if (offset + sizeof(std::uint32_t) > buffer.size())
        {
            return false;
        }

        std::uint32_t networkValue = 0;
        std::memcpy(&networkValue, buffer.data() + offset, sizeof(networkValue));
        value = ntohl(networkValue);
        offset += sizeof(networkValue);
        return true;
    }

    // Reads a double value from the buffer and advances the offset.
    bool readDouble(const std::vector<std::uint8_t> &buffer, std::size_t &offset, double &value)
    {
        if (offset + sizeof(std::uint64_t) > buffer.size())
        {
            return false;
        }

        std::uint64_t networkValue = 0;
        std::memcpy(&networkValue, buffer.data() + offset, sizeof(networkValue));
        const std::uint64_t hostValue = networkToHost64(networkValue);
        std::memcpy(&value, &hostValue, sizeof(value));
        offset += sizeof(networkValue);
        return true;
    }

    // Reads a length-prefixed string from the buffer and advances the offset.
    bool readString(const std::vector<std::uint8_t> &buffer, std::size_t &offset, std::string &value)
    {
        std::uint32_t stringLength = 0;
        if (!readUint32(buffer, offset, stringLength))
        {
            return false;
        }

        if (offset + stringLength > buffer.size())
        {
            return false;
        }

        value.assign(reinterpret_cast<const char *>(buffer.data() + offset), stringLength);
        offset += stringLength;
        return true;
    }

    // Creates the common request header containing the request id and operation id.
    std::vector<std::uint8_t> createRequestHeader(std::uint32_t requestId, Operation operation)
    {
        std::vector<std::uint8_t> request;
        appendUint32(request, requestId);
        appendUint32(request, static_cast<std::uint32_t>(operation));
        return request;
    }

    // Serializes a reply object into the agreed wire format.
    std::vector<std::uint8_t> serializeReply(const Reply &reply)
    {
        std::vector<std::uint8_t> buffer;
        appendUint32(buffer, reply.requestId);
        appendUint32(buffer, static_cast<std::uint32_t>(reply.status));
        appendString(buffer, reply.message);
        return buffer;
    }

    // Parses a reply from raw bytes and validates that the packet is fully consumed.
    bool deserializeReply(const std::vector<std::uint8_t> &buffer, Reply &reply)
    {
        std::size_t offset = 0;
        std::uint32_t statusValue = 0;

        if (!readUint32(buffer, offset, reply.requestId))
        {
            return false;
        }

        if (!readUint32(buffer, offset, statusValue))
        {
            return false;
        }

        if (!readString(buffer, offset, reply.message))
        {
            return false;
        }

        if (offset != buffer.size())
        {
            return false;
        }

        reply.status = static_cast<ReplyStatus>(statusValue);
        return true;
    }

} // namespace protocol
