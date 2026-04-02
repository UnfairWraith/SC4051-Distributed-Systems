package ServerJava;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class Protocol {
    public static final int OP_OPEN = 1;
    public static final int OP_CLOSE = 2;
    public static final int OP_DEPOSIT = 3;
    public static final int OP_WITHDRAW = 4;
    public static final int OP_MONITOR = 5;
    public static final int OP_HISTORY = 6;
    public static final int OP_TRANSFER = 7;

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_ERROR = 1;
    public static final int STATUS_UPDATE = 2;

    // Prevents instantiation of this protocol utility class.
    private Protocol() {
    }

    public static final class Reader {
        private final ByteBuffer buffer;

        // Wraps an incoming datagram in a big-endian buffer for sequential reads.
        public Reader(byte[] data, int length) {
            this.buffer = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN);
        }

        // Reads the next 32-bit integer field from the request buffer.
        public int readInt() {
            ensureRemaining(Integer.BYTES);
            return buffer.getInt();
        }

        // Reads the next double field from the request buffer.
        public double readDouble() {
            ensureRemaining(Double.BYTES);
            return buffer.getDouble();
        }

        // Reads a length-prefixed UTF-8 string from the request buffer.
        public String readString() {
            int length = readInt();
            if (length < 0) {
                throw new IllegalArgumentException("Negative string length.");
            }
            ensureRemaining(length);
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        // Reports whether unread bytes remain after parsing a request.
        public boolean hasRemaining() {
            return buffer.hasRemaining();
        }

        // Fails fast if the buffer does not contain enough bytes for the next field.
        private void ensureRemaining(int required) {
            if (buffer.remaining() < required) {
                throw new IllegalArgumentException("Malformed packet: insufficient bytes.");
            }
        }
    }

    public static final class Reply {
        private final int requestId;
        private final int status;
        private final String message;

        // Creates a reply object with the given request id, status, and message.
        public Reply(int requestId, int status, String message) {
            this.requestId = requestId;
            this.status = status;
            this.message = message;
        }

        // Returns the reply status so callers can apply special handling.
        public int getStatus() {
            return status;
        }

        // Serializes the reply into the agreed binary wire format.
        public byte[] toBytes() {
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer
                .allocate(Integer.BYTES + Integer.BYTES + Integer.BYTES + messageBytes.length)
                .order(ByteOrder.BIG_ENDIAN);

            buffer.putInt(requestId);
            buffer.putInt(status);
            buffer.putInt(messageBytes.length);
            buffer.put(messageBytes);
            return buffer.array();
        }
    }
}
