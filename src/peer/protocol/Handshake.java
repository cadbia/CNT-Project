package peer.protocol;

import java.nio.charset.StandardCharsets;
import peer.util.ByteUtils;

public class Handshake {
    public static final byte[] HEADER = "P2PFILESHARINGPROJ".getBytes(StandardCharsets.US_ASCII); // 18 bytes
    public static final int ZERO_BYTES = 10;

    public final int peerId;

    public Handshake(int peerId) { this.peerId = peerId; }

    public byte[] toBytes() {
        byte[] b = new byte[HEADER.length + ZERO_BYTES + 4];
        System.arraycopy(HEADER, 0, b, 0, HEADER.length);
        // ten zero bytes already default to 0
        ByteUtils.putInt(b, b.length - 4, peerId);
        return b;
    }

    public static Handshake parse(byte[] data) {
        if (data.length != HEADER.length + ZERO_BYTES + 4) {
            throw new IllegalArgumentException("Invalid handshake length: " + data.length);
        }
        for (int i = 0; i < HEADER.length; i++) {
            if (data[i] != HEADER[i]) throw new IllegalArgumentException("Bad handshake header");
        }
        int pid = ByteUtils.getInt(data, data.length - 4);
        return new Handshake(pid);
    }
}
