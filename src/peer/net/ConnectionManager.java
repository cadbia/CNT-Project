package peer.net;

import peer.config.CommonConfig;
import peer.config.PeerInfo;
import peer.config.PeerRow;
import peer.core.PieceManager;
import peer.log.PeerLogger;
import peer.protocol.Handshake;
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
            Socket s = null;
            PeerConnection pc = null;
            try {
                s = new Socket(r.host, r.port);
                // Perform handshake before creating PeerConnection
                int remote = performClientHandshake(s, r.peerId);
                if (remote != r.peerId) {
                    logger.info("Connect to " + r.peerId + " failed: Peer ID mismatch");
                    s.close();
                    continue;
                }
                // Create PeerConnection after successful handshake
                pc = new PeerConnection(r.peerId, s, logger);
                conns.put(r.peerId, pc);
                logger.connectedTo(r.peerId);

                // Midpoint: send a BITFIELD if we have any pieces
                if (pm.getPieceCount() > 0) {
                    byte[] bf = pm.getBitfield().toBytes();
                    pc.send(Message.of(MessageType.BITFIELD, bf));
                }
            } catch (Exception e) {
                logger.info("Connect to " + r.peerId + " failed: " + e.getMessage());
                // Clean up on failure
                if (pc != null) {
                    try { pc.close(); } catch (Exception ignored) {}
                } else if (s != null && !s.isClosed()) {
                    try { s.close(); } catch (Exception ignored) {}
                }
            }
        }
    }

    private void acceptLoop() {
        while (!server.isClosed()) {
            try {
                Socket s = server.accept();
                // Perform handshake before creating PeerConnection
                int remoteId = performServerHandshake(s);
                // Now create PeerConnection with the correct remote ID
                PeerConnection pc = new PeerConnection(remoteId, s, logger);
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

    private int performServerHandshake(Socket s) throws IOException {
        // Read incoming handshake from client
        byte[] buf = new byte[Handshake.HEADER.length + Handshake.ZERO_BYTES + 4];
        int off = 0;
        java.io.InputStream in = s.getInputStream();
        while (off < buf.length) {
            int r = in.read(buf, off, buf.length - off);
            if (r < 0) throw new IOException("EOF during handshake");
            off += r;
        }
        peer.protocol.Handshake hs = peer.protocol.Handshake.parse(buf);
        
        // Send handshake response
        peer.protocol.Handshake response = new peer.protocol.Handshake(selfId);
        s.getOutputStream().write(response.toBytes());
        s.getOutputStream().flush();
        
        return hs.peerId;
    }

    private int performClientHandshake(Socket s, int expectedPeerId) throws IOException {
        // Send handshake first (client initiates)
        peer.protocol.Handshake outgoing = new peer.protocol.Handshake(selfId);
        s.getOutputStream().write(outgoing.toBytes());
        s.getOutputStream().flush();
        
        // Read handshake response from server
        byte[] buf = new byte[Handshake.HEADER.length + Handshake.ZERO_BYTES + 4];
        int off = 0;
        java.io.InputStream in = s.getInputStream();
        while (off < buf.length) {
            int r = in.read(buf, off, buf.length - off);
            if (r < 0) throw new IOException("EOF during handshake");
            off += r;
        }
        peer.protocol.Handshake hs = peer.protocol.Handshake.parse(buf);
        
        return hs.peerId;
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
