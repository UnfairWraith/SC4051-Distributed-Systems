package Server;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Unmarshalling {
    // Unmarshal an int sent in big-endian (network order)
    public static int unmarshalInt(byte[] data, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, 4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getInt();
    }

    // Unmarshal a string (length-prefixed, big-endian)
    public static String unmarshalString(byte[] data, int offset, int length) {
        return new String(data, offset, length).trim();
    }

    // Unmarshal a BankOperation from a byte (ordinal sent by client)
    public static Operation unmarshalOperation(byte[] data, int offset) {
        int ordinal = data[offset] & 0xFF; // Ensure unsigned
        Operation[] ops = Operation.values();
        if (ordinal < 0 || ordinal >= ops.length) return null;
        return ops[ordinal];
    }

    // Unmarshal a requestId (int), operation (byte), and account (string)
    // [0-3]: requestId (int, 4 bytes)
    // [4]: operation ordinal (byte, 1 byte)
    // [5-end]: account string (remaining bytes)
    public static BankRequest unmarshalRequest(byte[] data, java.net.InetAddress clientAddress, int clientPort) {
        int requestId = unmarshalInt(data, 0);
        Operation operation = unmarshalOperation(data, 4);
        String account = unmarshalString(data, 5, data.length - 5);

        // You may want to extend BankRequest to include operation/account if needed
        BankRequest req = new BankRequest(requestId, clientAddress, clientPort);
        // Optionally, set additional fields on req if you add them to BankRequest
        // e.g., req.setOperation(operation); req.setAccount(account);
        return req;
    }
}
