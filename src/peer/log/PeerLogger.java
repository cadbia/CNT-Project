package peer.log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PeerLogger implements AutoCloseable {
    private final BufferedWriter bw;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PeerLogger(int peerId, String workDir) throws IOException {
        Files.createDirectories(Path.of(workDir));
        String logPath = Path.of(workDir, "log_peer_" + peerId + ".log").toString();
        bw = new BufferedWriter(new FileWriter(logPath, true));
    }

    public synchronized void info(String s) {
        try {
            bw.write("[" + LocalDateTime.now().format(fmt) + "] " + s);
            bw.newLine();
            bw.flush();
        } catch (IOException ignored) {}
    }

    public void connectedTo(int remote) { info("make a connection to Peer " + remote + "."); }
    public void connectedFrom(int remote) { info("is connected from Peer " + remote + "."); }
    public void changedPreferredNeighbors(List<Integer> ids) { info("has the preferred neighbors " + ids + "."); }
    public void changedOptimisticNeighbor(int id) { info("has the optimistically unchoked neighbor " + id + "."); }
    public void choked(int id) { info("is choked by " + id + "."); }
    public void unchoked(int id) { info("is unchoked by " + id + "."); }
    public void interested(int id) { info("received the 'interested' message from " + id + "."); }
    public void notInterested(int id) { info("received the 'not interested' message from " + id + "."); }
    public void have(int id, int pieceIdx) { info("received the 'have' message from " + id + " for the piece " + pieceIdx + "."); }
    public void downloadedPiece(int id, int pieceIdx, int haveCount) { info("has downloaded the piece " + pieceIdx + " from " + id + ". Now the number of pieces it has is " + haveCount + "."); }
    public void completed() { info("has downloaded the complete file."); }
    public void remoteCompleted(int remoteId) { info("confirms Peer " + remoteId + " has downloaded the complete file."); }
    public void terminated() { info("terminates because all peers now have the complete file."); }

    @Override
    public void close() throws Exception {
        bw.close();
    }
}
