package p2p.common;
import java.nio.file.*; import java.util.*;
public final class PeerTable {
  public final java.util.List<PeerInfo> peers;
  public PeerTable(java.util.List<PeerInfo> ps){ this.peers = List.copyOf(ps); }
  public static PeerTable load(Path path) throws Exception {
    var list = new ArrayList<PeerInfo>();
    for (String line : java.nio.file.Files.readAllLines(path)) {
      line = line.trim(); if (line.isEmpty()) continue;
      String[] t = line.split("\\s+");
      list.add(new PeerInfo(Integer.parseInt(t[0]), t[1], Integer.parseInt(t[2]), t[3].equals("1")));
    }
    return new PeerTable(list);
  }
  public java.util.Optional<PeerInfo> byId(int id){ return peers.stream().filter(p->p.id()==id).findFirst(); }
  public java.util.List<PeerInfo> earlierThan(int id){ return peers.stream().filter(p->p.id()<id).toList(); }
}

