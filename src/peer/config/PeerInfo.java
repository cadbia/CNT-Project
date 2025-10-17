package peer.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PeerInfo {
    private final List<PeerRow> rows;
    private PeerInfo(List<PeerRow> rows) { this.rows = rows; }

    public static PeerInfo load(String path) throws Exception {
        List<PeerRow> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\s+");
                if (parts.length >= 4) {
                    rows.add(new PeerRow(
                            Integer.parseInt(parts[0]),
                            parts[1],
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3])
                    ));
                }
            }
        }
        return new PeerInfo(rows);
    }

    public PeerRow self(int peerId) {
        for (PeerRow r : rows) if (r.peerId == peerId) return r;
        return null;
    }

    public List<PeerRow> all() { return rows; }

    public List<PeerRow> earlierThan(int peerId) {
        return rows.stream().filter(r -> r.peerId < peerId).collect(Collectors.toList());
    }

    public List<PeerRow> laterThan(int peerId) {
        return rows.stream().filter(r -> r.peerId > peerId).collect(Collectors.toList());
    }
}
