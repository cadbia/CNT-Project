package peer.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class CommonConfig {
    public final int numberOfPreferredNeighbors;
    public final int unchokingInterval;
    public final int optimisticUnchokingInterval;
    public final String fileName;
    public final int fileSize;
    public final int pieceSize;

    private CommonConfig(Map<String, String> kv) {
        this.numberOfPreferredNeighbors = Integer.parseInt(kv.getOrDefault("NumberOfPreferredNeighbors", "2"));
        this.unchokingInterval = Integer.parseInt(kv.getOrDefault("UnchokingInterval", "5"));
        this.optimisticUnchokingInterval = Integer.parseInt(kv.getOrDefault("OptimisticUnchokingInterval", "15"));
        this.fileName = kv.getOrDefault("FileName", "sample.data");
        this.fileSize = Integer.parseInt(kv.getOrDefault("FileSize", "0"));
        this.pieceSize = Integer.parseInt(kv.getOrDefault("PieceSize", "16384"));
    }

    public static CommonConfig load(String path) throws Exception {
        Map<String, String> kv = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\s+");
                if (parts.length >= 2) {
                    kv.put(parts[0], parts[1]);
                }
            }
        }
        return new CommonConfig(kv);
    }

    public int pieceCount() {
        if (pieceSize <= 0) return 0;
        return (fileSize + pieceSize - 1) / pieceSize;
    }

    public int lastPieceSize() {
        int pc = pieceCount();
        if (pc == 0) return 0;
        int fullBytes = (pc - 1) * pieceSize;
        int last = fileSize - fullBytes;
        return last <= 0 ? pieceSize : last;
    }
}
