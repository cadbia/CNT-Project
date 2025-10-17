package peer.core;

import peer.util.ByteUtils;

import java.util.ArrayList;
import java.util.List;

public class Bitfield {
    private final int pieceCount;
    private final byte[] bits;

    public Bitfield(int pieceCount, boolean hasAll) {
        this.pieceCount = pieceCount;
        int bytes = (pieceCount + 7) / 8;
        this.bits = new byte[bytes];
        if (hasAll) {
            for (int i = 0; i < pieceCount; i++) setPiece(i);
        }
    }

    public boolean hasPiece(int idx) {
        return ByteUtils.hasBit(bits, idx);
    }
    public void setPiece(int idx) {
        ByteUtils.setBit(bits, idx);
    }
    public byte[] toBytes() {
        return bits.clone();
    }
    public static Bitfield fromBytes(byte[] b, int pieceCount) {
        Bitfield f = new Bitfield(pieceCount, false);
        int len = Math.min(f.bits.length, b.length);
        System.arraycopy(b, 0, f.bits, 0, len);
        return f;
    }
    public int countHave() {
        int c = 0;
        for (int i = 0; i < pieceCount; i++) if (hasPiece(i)) c++;
        return c;
    }
    public List<Integer> missingComparedTo(Bitfield other) {
        List<Integer> xs = new ArrayList<>();
        for (int i = 0; i < pieceCount; i++) {
            if (other.hasPiece(i) && !this.hasPiece(i)) xs.add(i);
        }
        return xs;
    }
    public int pieceCount() { return pieceCount; }
}
