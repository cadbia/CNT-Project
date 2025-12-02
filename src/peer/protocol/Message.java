package peer.protocol;

import peer.util.ByteUtils;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class Message {
    public final int length; // excludes the 4-byte length
    public final byte type;
    public final byte[] payload;

    public Message(int length, byte type, byte[] payload) {
        this.length = length;
        this.type = type;
        this.payload = payload == null ? new byte[0] : payload;
    }

    // factory method
    public static Message of(MessageType t, byte[] payload) {
        int len = 1 + (payload == null ? 0 : payload.length);
        return new Message(len, t.code, payload);
    }

    public byte[] toBytes() {
        byte[] out = new byte[4 + length];
        ByteUtils.putInt(out, 0, length);
        out[4] = type;
        if (payload != null && payload.length > 0) {
            System.arraycopy(payload, 0, out, 5, payload.length);
        }
        return out;
    }

    // factory method to parse from socket stream
    public static Message parse(InputStream in) throws IOException {
        DataInputStream din = new DataInputStream(in);
        int len;
        try {
            len = din.readInt();
        } catch (EOFException e) {
            return null;
        }
        if (len < 0) throw new IOException("Negative length");
        byte type = din.readByte();
        byte[] payload = new byte[Math.max(0, len - 1)];
        din.readFully(payload);
        return new Message(len, type, payload);
    }

    @Override
    public String toString() {
        return "Message{len=" + length + ", type=" + type + ", payload=" + Arrays.toString(payload) + "}";
    }
}
