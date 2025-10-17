package p2p.common;
import java.nio.file.*; import java.util.*;
public final class CommonConfig {
  public final int numberOfPreferredNeighbors;
  public final int unchokingIntervalSec;
  public final int optimisticUnchokingIntervalSec;
  public final String fileName;
  public final long fileSize;
  public final int pieceSize;
  public final int numPieces; public final int lastPieceSize;

  private CommonConfig(Map<String,String> m) {
    numberOfPreferredNeighbors = Integer.parseInt(m.get("NumberOfPreferredNeighbors"));
    unchokingIntervalSec = Integer.parseInt(m.get("UnchokingInterval"));
    optimisticUnchokingIntervalSec = Integer.parseInt(m.get("OptimisticUnchokingInterval"));
    fileName = m.get("FileName"); fileSize = Long.parseLong(m.get("FileSize"));
    pieceSize = Integer.parseInt(m.get("PieceSize"));
    numPieces = (int)((fileSize + pieceSize - 1) / pieceSize);
    lastPieceSize = (int)(fileSize - (long)(numPieces - 1) * pieceSize);
  }
  public static CommonConfig load(Path path) throws Exception {
    Map<String,String> m = new HashMap<>();
    for (String line : Files.readAllLines(path)) {
      line = line.trim(); if (line.isEmpty()) continue;
      String[] kv = line.split("\\s+", 2); m.put(kv[0], kv[1]);
    }
    return new CommonConfig(m);
  }
}
