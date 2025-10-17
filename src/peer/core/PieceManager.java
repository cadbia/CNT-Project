package peer.core;

import peer.config.CommonConfig;

public class PieceManager {
    private final Bitfield bitfield;
    private final int pieceCount;
    private final int pieceSize;
    private final String fileName;
    // simple in-memory backing for midpoint
    private final byte[][] pieces;

    public PieceManager(CommonConfig cfg, String workDir, boolean hasFile) {
        this.pieceCount = cfg.pieceCount();
        this.pieceSize = cfg.pieceSize;
        this.fileName = cfg.fileName;
        this.bitfield = new Bitfield(pieceCount, hasFile);
        this.pieces = new byte[pieceCount][];
        if (hasFile) {
            for (int i = 0; i < pieceCount; i++) {
                int size = (i == pieceCount - 1) ? cfg.lastPieceSize() : pieceSize;
                this.pieces[i] = new byte[size]; // dummy bytes
            }
        }
    }

    public synchronized byte[] readPiece(int idx) {
        if (idx < 0 || idx >= pieceCount) return null;
        return pieces[idx];
    }
    public synchronized void writePiece(int idx, byte[] data) {
        if (idx < 0 || idx >= pieceCount) return;
        pieces[idx] = data;
        bitfield.setPiece(idx);
    }
    public boolean isComplete() {
        return bitfield.countHave() == pieceCount;
    }
    public Bitfield getBitfield() { return bitfield; }
    public int getPieceCount() { return pieceCount; }
}
