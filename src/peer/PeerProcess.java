package peer;

import peer.config.CommonConfig;
import peer.config.PeerInfo;
import peer.config.PeerRow;
import peer.core.PieceManager;
import peer.log.PeerLogger;
import peer.net.ConnectionManager;

import java.nio.file.Files;
import java.nio.file.Path;

public class PeerProcess {
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java peer.PeerProcess <peerID> [configDir]");
            System.exit(2);
        }
        int selfId = Integer.parseInt(args[0]);
        String configDir = args.length == 2 ? args[1] : "resources";
        Path configPath = Path.of(configDir);
        Path commonPath = configPath.resolve("Common.cfg");
        Path peerInfoPath = configPath.resolve("PeerInfo.cfg");

        CommonConfig common = CommonConfig.load(commonPath.toString());
        PeerInfo peerInfo = PeerInfo.load(peerInfoPath.toString());

        PeerRow me = peerInfo.self(selfId);
        if (me == null) {
            throw new IllegalArgumentException("PeerID " + selfId + " not found in PeerInfo.cfg");
        }

        Path workDir = Path.of("peer_" + selfId);
        Files.createDirectories(workDir);

        try (PeerLogger logger = new PeerLogger(selfId, workDir.toString())) {
            PieceManager pm = new PieceManager(common, workDir.toString(), me.hasFile == 1);
            logger.info("Peer " + selfId + " started. Config dir=" + configDir + ", pieces=" + pm.getPieceCount());

            ConnectionManager cm = new ConnectionManager(selfId, me.port, peerInfo, common, pm, logger);

            Thread shutdownHook = new Thread(() -> {
                try {
                    logger.info("Shutdown requested.");
                    cm.stop();
                } catch (Exception ignored) {}
            }, "shutdown-" + selfId);
            Runtime.getRuntime().addShutdownHook(shutdownHook);

            try {
                cm.start();
                cm.awaitCompletion();
            } finally {
                cm.stop();
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (IllegalStateException ignored) {
                    // JVM already shutting down
                }
            }
        }
    }
}
