package peer;

import peer.config.CommonConfig;
import peer.config.PeerInfo;
import peer.config.PeerRow;
import peer.core.PieceManager;
import peer.log.PeerLogger;
import peer.net.ConnectionManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PeerProcess {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java peer.PeerProcess <peerID>");
            System.exit(2);
        }
        int selfId = Integer.parseInt(args[0]);

        CommonConfig common = CommonConfig.load("resources/Common.cfg");
        PeerInfo peerInfo = PeerInfo.load("resources/PeerInfo.cfg");

        PeerRow me = peerInfo.self(selfId);
        if (me == null) {
            throw new IllegalArgumentException("PeerID " + selfId + " not found in PeerInfo.cfg");
        }

        // Ensure working dir peer_<id>/
        Path workDir = Path.of("peer_" + selfId);
        Files.createDirectories(workDir);

        // Logger
        PeerLogger logger = new PeerLogger(selfId, workDir.toString());

        // Piece manager (in-memory for midpoint)
        PieceManager pm = new PieceManager(common, workDir.toString(), me.hasFile == 1);
        logger.info("Peer " + selfId + " started. pieceCount=" + pm.getPieceCount());

        // Connection manager (scaffold)
        ConnectionManager cm = new ConnectionManager(selfId, me.port, peerInfo, common, pm, logger);
        cm.start();

        // Basic lifecycle: keep process alive until CTRL-C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.info("Shutdown requested.");
                cm.stop();
                logger.close();
            } catch (Exception ignored) {}
        }));

        // Idle loop (midpoint)
        while (true) {
            Thread.sleep(15000);
            if (pm.isComplete()) {
                logger.completed();
                // NOTE: In full project you would check if everyone is complete before exiting.
                // For midpoint, keep running to keep sockets alive.
            }
        }
    }
}
