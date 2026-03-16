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

    private Protocol() {
    }

    public static final class Reader {
        private final ByteBuffer buffer;

        public Reader(byte[] data, int length) {
            this.buffer = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN);
        }

        public int readInt() {
            ensureRemaining(Integer.BYTES);
            return buffer.getInt();
        }

        public double readDouble() {
            ensureRemaining(Double.BYTES);
            return buffer.getDouble();
        }

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

        public boolean hasRemaining() {
            return buffer.hasRemaining();
        }

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

        public Reply(int requestId, int status, String message) {
            this.requestId = requestId;
            this.status = status;
            this.message = message;
        }

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
