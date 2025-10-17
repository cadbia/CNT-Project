package peer.config;

public class PeerRow {
    public final int peerId;
    public final String host;
    public final int port;
    public final int hasFile;
    public PeerRow(int peerId, String host, int port, int hasFile) {
        this.peerId = peerId;
        this.host = host;
        this.port = port;
        this.hasFile = hasFile;
    }
    @Override
    public String toString() {
        return "PeerRow{" + peerId + " " + host + ":" + port + " hasFile=" + hasFile + "}";
    }
}
