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
    public static OperationRequest unmarshalRequest(byte[] data, java.net.InetAddress clientAddress, int clientPort) {
        int requestId = unmarshalInt(data, 0);
        Operation operation = unmarshalOperation(data, 4);

        String name = null;
        String password = null;
        String currencyType = null;
        String account = null;
        String destAccount = null;
        int amount = 0;
        int balance = 0;
        int timeInterval = 0;

        switch (operation) {
            case OPEN_ACCT:
                // [5-24]: name (20 bytes)
                // [25-44]: password (20 bytes)
                // [45-54]: currencyType (10 bytes)
                // [55-58]: balance (int, 4 bytes)
                name = unmarshalString(data, 5, 20);
                password = unmarshalString(data, 25, 20);
                currencyType = unmarshalString(data, 45, 10);
                balance = unmarshalInt(data, 55);
                break;
            case CLOSE_ACCT:
                // [5-24]: name (20 bytes)
                // [25-44]: accountNumber (20 bytes)
                // [45-64]: password (20 bytes)
                name = unmarshalString(data, 5, 20);
                account = unmarshalString(data, 25, 20);
                password = unmarshalString(data, 45, 20);
                break;
            case DEPOSIT:
            case WITHDRAW:
                // [5-24]: name (20 bytes)
                // [25-44]: accountNumber (20 bytes)
                // [45-64]: password (20 bytes)
                // [65-74]: currencyType (10 bytes)
                // [75-78]: amount (int, 4 bytes)
                name = unmarshalString(data, 5, 20);
                account = unmarshalString(data, 25, 20);
                password = unmarshalString(data, 45, 20);
                currencyType = unmarshalString(data, 65, 10);
                amount = unmarshalInt(data, 75);
                break;
            case MONITOR:
                // [5-8]: timeInterval (int, 4 bytes)
                timeInterval = unmarshalInt(data, 5);
                break;
            case VIEW_TX_HISTORY:
                // [5-24]: name (20 bytes)
                // [25-44]: accountNumber (20 bytes)
                // [45-64]: password (20 bytes)
                name = unmarshalString(data, 5, 20);
                account = unmarshalString(data, 25, 20);
                password = unmarshalString(data, 45, 20);
                break;
            case TRANSFER:
                // [5-24]: name (20 bytes)
                // [25-44]: password (20 bytes)
                // [45-64]: sourceAccountNumber (20 bytes)
                // [65-84]: destAccountNumber (20 bytes)
                // [85-94]: currencyType (10 bytes)
                // [95-98]: amount (int, 4 bytes)
                name = unmarshalString(data, 5, 20);
                password = unmarshalString(data, 25, 20);
                account = unmarshalString(data, 45, 20);
                destAccount = unmarshalString(data, 65, 20);
                currencyType = unmarshalString(data, 85, 10);
                amount = unmarshalInt(data, 95);
                break;
            default:
                // Handle unknown operation if needed
                break;
        }

        OperationRequest opReq = new OperationRequest(operation, requestId, timeInterval,
            clientAddress.getHostAddress(), clientPort,
            amount, account, destAccount);
        return opReq;
    }
}
