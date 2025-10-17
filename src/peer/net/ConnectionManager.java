package peer.net;

import peer.config.CommonConfig;
import peer.config.PeerInfo;
import peer.config.PeerRow;
import peer.core.PieceManager;
import peer.log.PeerLogger;
import peer.protocol.Message;
import peer.protocol.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionManager {
    private final int selfId;
    private final int selfPort;
    private final PeerInfo info;
    private final CommonConfig common;
    private final PieceManager pm;
    private final PeerLogger logger;

    private final Map<Integer, PeerConnection> conns = new HashMap<>();
    private final ExecutorService acceptor = Executors.newSingleThreadExecutor();
    private ServerSocket server;

    public ConnectionManager(int selfId, int selfPort, PeerInfo info, CommonConfig common, PieceManager pm, PeerLogger logger) {
        this.selfId = selfId;
        this.selfPort = selfPort;
        this.info = info;
        this.common = common;
        this.pm = pm;
        this.logger = logger;
    }

    public void start() throws IOException {
        // Start server
        server = new ServerSocket(selfPort);
        acceptor.submit(this::acceptLoop);

        // Connect to earlier peers
        for (PeerRow r : info.earlierThan(selfId)) {
            try {
                Socket s = new Socket(r.host, r.port);
                PeerConnection pc = new PeerConnection(r.peerId, s, logger);
                pc.sendHandshake(selfId);
                int remote = pc.recvHandshake();
                if (remote != r.peerId) {
                    pc.close();
                    continue;
                }
                conns.put(r.peerId, pc);
                logger.connectedTo(r.peerId);

                // Midpoint: send a BITFIELD if we have any pieces
                if (pm.getPieceCount() > 0) {
                    byte[] bf = pm.getBitfield().toBytes();
                    pc.send(Message.of(MessageType.BITFIELD, bf));
                }
            } catch (Exception e) {
                logger.info("Connect to " + r.peerId + " failed: " + e.getMessage());
            }
        }
    }

    private void acceptLoop() {
        while (!server.isClosed()) {
            try {
                Socket s = server.accept();
                // read handshake first
                PeerConnection pc = new PeerConnection(-1, s, logger);
                int remoteId = pc.recvHandshake();
                // respond
                pc.sendHandshake(selfId);
                conns.put(remoteId, pc);
                logger.connectedFrom(remoteId);

                // Midpoint: send our bitfield
                if (pm.getPieceCount() > 0) {
                    byte[] bf = pm.getBitfield().toBytes();
                    pc.send(Message.of(MessageType.BITFIELD, bf));
                }
            } catch (Exception e) {
                if (!server.isClosed()) logger.info("Accept failed: " + e.getMessage());
            }
        }
    }

    public void stop() throws IOException {
        server.close();
        acceptor.shutdownNow();
        for (PeerConnection pc : conns.values()) {
            try { pc.close(); } catch (Exception ignored) {}
        }
        conns.clear();
    }
}
