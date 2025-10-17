package peer.protocol;

public enum MessageType {
    CHOKE(0), UNCHOKE(1), INTERESTED(2), NOT_INTERESTED(3),
    HAVE(4), BITFIELD(5), REQUEST(6), PIECE(7);

    public final byte code;
    MessageType(int code) { this.code = (byte) code; }

    public static MessageType from(byte b) {
        for (MessageType t : values()) if (t.code == b) return t;
        throw new IllegalArgumentException("Unknown message type: " + b);
    }
}
