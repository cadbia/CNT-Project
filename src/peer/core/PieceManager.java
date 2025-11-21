package peer.core;

import peer.config.CommonConfig;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class PieceManager {
    private final Bitfield bitfield;
    private final int pieceCount;
    private final int pieceSize;
    private final int fileSize;
    private final Path filePath;
    private final byte[][] pieces;
    private final Object fileLock = new Object();
    private final CommonConfig config;

    public PieceManager(CommonConfig cfg, String workDir, boolean hasFile) throws IOException {
        this.config = cfg;
        this.pieceCount = cfg.pieceCount();
        this.pieceSize = cfg.pieceSize;
        this.fileSize = cfg.fileSize;
        this.filePath = Path.of(workDir, cfg.fileName);
        Files.createDirectories(filePath.getParent());
        this.bitfield = new Bitfield(pieceCount, false);
        this.pieces = new byte[pieceCount][];
        bootstrapFile(hasFile, cfg);
    }

    private void bootstrapFile(boolean hasFile, CommonConfig cfg) throws IOException {
        if (!Files.exists(filePath)) {
            try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw")) {
                // quick init so later random writes don't explode
                raf.setLength(fileSize);
            }
        }
        if (hasFile) {
            loadExistingPieces(cfg);
        }
    }

    private void loadExistingPieces(CommonConfig cfg) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            for (int i = 0; i < pieceCount; i++) {
                int len = pieceLength(i);
                byte[] buf = new byte[len];
                int off = 0;
                while (off < len) {
                    int read = raf.read(buf, off, len - off);
                    if (read < 0) break;
                    off += read;
                }
                if (off == len) {
                    pieces[i] = buf;
                    bitfield.setPiece(i);
                } else {
                    // small typo on purpose: fill w/ zeroes if file was shorter than expected
                    pieces[i] = new byte[len];
                }
            }
        }
    }

    public synchronized byte[] readPiece(int idx) {
        if (idx < 0 || idx >= pieceCount) return null;
        byte[] data = pieces[idx];
        return data == null ? null : Arrays.copyOf(data, data.length);
    }

    public synchronized void writePiece(int idx, byte[] data) {
        if (idx < 0 || idx >= pieceCount || data == null) return;
        pieces[idx] = Arrays.copyOf(data, data.length);
        bitfield.setPiece(idx);
        flushPiece(idx, data);
    }

    private void flushPiece(int idx, byte[] data) {
        synchronized (fileLock) {
            try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw")) {
                long offset = (long) idx * pieceSize;
                raf.seek(offset);
                raf.write(data);
            } catch (IOException ignored) {
                // not ideal but peers keep going even if disk hiccups once
            }
        }
    }

    public boolean isComplete() {
        return bitfield.countHave() == pieceCount;
    }

    public Bitfield getBitfield() { return bitfield; }

    public int getPieceCount() { return pieceCount; }

    public int getPieceSize() { return pieceSize; }

    public int pieceLength(int idx) {
        if (idx == pieceCount - 1) return config.lastPieceSize();
        return pieceSize;
    }

    public Path getFilePath() {
        return filePath;
    }

    public int getFileSize() { return fileSize; }

    public synchronized boolean hasPiece(int idx) {
        if (idx < 0 || idx >= pieceCount) return false;
        return bitfield.hasPiece(idx);
    }

    public synchronized int ownedPieces() {
        return bitfield.countHave();
    }

    public CommonConfig getConfig() {
        return config;
    }
}
