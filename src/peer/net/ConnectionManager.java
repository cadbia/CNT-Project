package peer.net;

import peer.config.CommonConfig;
import peer.config.PeerInfo;
import peer.config.PeerRow;
import peer.core.Bitfield;
import peer.core.ChokeManager;
import peer.core.PieceManager;
import peer.log.PeerLogger;
import peer.protocol.Handshake;
import peer.protocol.Message;
import peer.protocol.MessageType;
import peer.util.ByteUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionManager implements MessageHandler {
    private final int selfId;
    private final int selfPort;
    private final PeerInfo info;
    private final CommonConfig common;
    private final PieceManager pm;
    private final PeerLogger logger;
    private final Map<Integer, PeerSession> sessions = new ConcurrentHashMap<>();
    private final ExecutorService acceptor;
    private final ExecutorService inboundExecutor;
    private final Object stateLock = new Object();
    private final Set<Integer> preferredNeighbors = new HashSet<>();
    private final Set<Integer> outstandingPieces = ConcurrentHashMap.newKeySet();
    private final Set<Integer> completedPeers = ConcurrentHashMap.newKeySet();
    private final CountDownLatch completionLatch = new CountDownLatch(1);
    private final Random random = new Random();
    private final ChokeManager chokeManager;
    private final AtomicBoolean selfCompletionLogged;
    private ServerSocket server;
    private volatile boolean shutdown;
    private Integer optimisticNeighbor = null;

    public ConnectionManager(int selfId, int selfPort, PeerInfo info, CommonConfig common, PieceManager pm, PeerLogger logger) {
        this.selfId = selfId;
        this.selfPort = selfPort;
        this.info = info;
        this.common = common;
        this.pm = pm;
        this.logger = logger;
        this.acceptor = Executors.newSingleThreadExecutor(r -> new Thread(r, "acceptor-" + selfId));
        this.inboundExecutor = Executors.newCachedThreadPool(r -> new Thread(r, "msg-" + selfId + "-" + r.hashCode()));
        this.chokeManager = new ChokeManager(common.unchokingInterval, common.optimisticUnchokingInterval, 
            this::handlePreferredTick, this::handleOptimisticTick);
        this.selfCompletionLogged = new AtomicBoolean(pm.isComplete());
            if (pm.isComplete()) {
                completedPeers.add(selfId);
            }
    }

    public void start() throws IOException {
        server = new ServerSocket(selfPort);
        acceptor.submit(this::acceptLoop);
        for (PeerRow row : info.earlierThan(selfId)) {
            dialPeer(row);
        }
        chokeManager.start();
    }

    private void dialPeer(PeerRow row) {
        CompletableFuture.runAsync(() -> {
            try {
                Socket socket = new Socket(row.host, row.port);
                int remote = performClientHandshake(socket);
                if (remote != row.peerId) {
                    logger.info("connection refused – expected " + row.peerId + " got " + remote);
                    socket.close();
                    return;
                }
                registerConnection(remote, socket, true);
            } catch (Exception ex) {
                logger.info("Connect to " + row.peerId + " failed: " + ex.getMessage());
            }
        });
    }

    private void acceptLoop() {
        while (!shutdown) {
            try {
                Socket socket = server.accept();
                int remoteId = performServerHandshake(socket);
                registerConnection(remoteId, socket, false);
            } catch (Exception ex) {
                if (!shutdown) {
                    logger.info("Accept failed: " + ex.getMessage());
                }
            }
        }
    }

    private void registerConnection(int remoteId, Socket socket, boolean initiatedByUs) throws IOException {
        PeerRow row = info.self(remoteId);
        if (row == null) {
            socket.close();
            throw new IOException("Peer " + remoteId + " not in config");
        }
        PeerSession session = sessions.computeIfAbsent(remoteId, id -> new PeerSession(row));
        synchronized (session) {
            if (session.connection != null) {
                try { session.connection.close(); } catch (Exception ignored) {}
            }
            session.connection = new PeerConnection(remoteId, socket, logger, this);
            session.disconnected = false;
        }
        if (initiatedByUs) {
            logger.connectedTo(remoteId);
        } else {
            logger.connectedFrom(remoteId);
        }
        sendBitfield(session);
        evaluateInterest(session);
    }

    private void sendBitfield(PeerSession session) {
        if (pm.getPieceCount() == 0) return;
        Message bitfield = Message.of(MessageType.BITFIELD, pm.getBitfield().toBytes());
        session.send(bitfield);
    }

    // send message based on interest
    private void evaluateInterest(PeerSession session) {
        synchronized (session) {
            boolean interesting = hasInterestingPieces(session);
            if (interesting && !session.weAreInterested) {
                session.weAreInterested = true;
                session.send(Message.of(MessageType.INTERESTED, null));
            } else if (!interesting && session.weAreInterested) {
                session.weAreInterested = false;
                session.send(Message.of(MessageType.NOT_INTERESTED, null));
            }
        }
    }

    // check if peer has interesting pieces
    private boolean hasInterestingPieces(PeerSession session) {
        Bitfield remote = session.remoteBitfield;
        Bitfield local = pm.getBitfield();
        int pieces = pm.getPieceCount();
        for (int i = 0; i < pieces; i++) {
            if (remote.hasPiece(i) && !local.hasPiece(i) && !outstandingPieces.contains(i)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMessage(int remotePeerId, Message message) {
        inboundExecutor.submit(() -> processMessage(remotePeerId, message));
    }

    @Override
    public void onDisconnect(int remotePeerId) {
        PeerSession session = sessions.get(remotePeerId);
        if (session != null) {
            synchronized (session) {
                session.disconnected = true;
                session.pendingRequest = null;
            }
        }
    }

    private void processMessage(int remotePeerId, Message message) {
        PeerSession session = sessions.get(remotePeerId);
        if (session == null) return;
        MessageType type = MessageType.from(message.type);
        switch (type) {
            case CHOKE -> handleChoke(session, true);
            case UNCHOKE -> handleChoke(session, false);
            case INTERESTED -> handleInterest(session, true);
            case NOT_INTERESTED -> handleInterest(session, false);
            case HAVE -> handleHave(session, message.payload);
            case BITFIELD -> handleBitfield(session, message.payload);
            case REQUEST -> handleRequest(session, message.payload);
            case PIECE -> handlePiece(session, message.payload);
            default -> {}
        }
    }

    private void handleChoke(PeerSession session, boolean choked) {
        synchronized (session) {
            session.remoteChokesUs = choked;
            if (choked) {
                if (session.pendingRequest != null) {
                    outstandingPieces.remove(session.pendingRequest);
                    session.pendingRequest = null;
                }
                logger.choked(session.peerId);
            } else {
                logger.unchoked(session.peerId);
                requestNextPiece(session);
            }
        }
    }

    // handles interested and not_interested
    private void handleInterest(PeerSession session, boolean interested) {
        synchronized (session) {
            session.remoteInterested = interested;
        }
        if (interested) {
            logger.interested(session.peerId);
        } else {
            logger.notInterested(session.peerId);
        }
    }

    private void handleHave(PeerSession session, byte[] payload) {
        int piece = ByteUtils.getInt(payload, 0);
        synchronized (session) {
            session.remoteBitfield.setPiece(piece);
        }
        logger.have(session.peerId, piece);
        evaluateInterest(session);
        maybeMarkRemoteComplete(session);
    }

    private void handleBitfield(PeerSession session, byte[] payload) {
        synchronized (session) {
            session.remoteBitfield = Bitfield.fromBytes(payload, pm.getPieceCount());
        }
        evaluateInterest(session);
        maybeMarkRemoteComplete(session);
    }

    private void handleRequest(PeerSession session, byte[] payload) {
        int piece = ByteUtils.getInt(payload, 0); // piece ID
        boolean allowed;
        synchronized (session) {
            allowed = !session.weChokeRemote && pm.hasPiece(piece);
        }
        if (allowed) {
            // read piece and send to peer
            byte[] data = pm.readPiece(piece);
            if (data != null) {
                byte[] buff = new byte[4 + data.length];
                ByteUtils.putInt(buff, 0, piece);
                System.arraycopy(data, 0, buff, 4, data.length);
                session.send(Message.of(MessageType.PIECE, buff));
            }
        }
    }

    private void handlePiece(PeerSession session, byte[] payload) {
        if (payload.length < 4) return;
        int pieceIdx = ByteUtils.getInt(payload, 0);
        byte[] data = new byte[payload.length - 4];
        System.arraycopy(payload, 4, data, 0, data.length);
        synchronized (session) {
            if (session.pendingRequest != null && session.pendingRequest == pieceIdx) {
                outstandingPieces.remove(pieceIdx);
                session.pendingRequest = null;
            }
            session.bytesDownloaded += data.length;
        }
        pm.writePiece(pieceIdx, data);
        logger.downloadedPiece(session.peerId, pieceIdx, pm.ownedPieces());
        if (pm.isComplete()) {
            completedPeers.add(selfId);
            if (selfCompletionLogged.compareAndSet(false, true)) {
                logger.completed();
            }
        }
        broadcastHave(pieceIdx);
        checkForCompletion();
        requestNextPiece(session);
    }

    private void requestNextPiece(PeerSession session) {
        synchronized (session) {
            if (session.remoteChokesUs) return;
            if (session.pendingRequest != null) return;
            List<Integer> candidates = new ArrayList<>();
            int total = pm.getPieceCount();
            for (int i = 0; i < total; i++) {
                if (session.remoteBitfield.hasPiece(i) && !pm.hasPiece(i) && !outstandingPieces.contains(i)) {
                    candidates.add(i);
                }
            }
            if (candidates.isEmpty()) {
                evaluateInterest(session);
                return;
            }
            Collections.shuffle(candidates, random);
            int pick = candidates.get(0);
            byte[] payload = new byte[4];
            ByteUtils.putInt(payload, 0, pick);
            session.pendingRequest = pick;
            outstandingPieces.add(pick);
            session.send(Message.of(MessageType.REQUEST, payload));
        }
    }

    private void broadcastHave(int pieceIdx) {
        byte[] payload = new byte[4];
        ByteUtils.putInt(payload, 0, pieceIdx);
        Message have = Message.of(MessageType.HAVE, payload);
        for (PeerSession session : sessions.values()) {
            session.send(have);
            evaluateInterest(session);
        }
    }

    private void handlePreferredTick() {
        synchronized (stateLock) {
            List<PeerSession> interested = sessions.values().stream()
                    .filter(s -> s.remoteInterested && !s.disconnected)
                    .collect(Collectors.toList());
            if (interested.isEmpty()) {
                preferredNeighbors.clear();
                return;
            }
            List<PeerSession> winners = new ArrayList<>(interested);
            if (pm.isComplete()) {
                Collections.shuffle(winners, random);
            } else {
                winners.sort(Comparator.comparingLong((PeerSession s) -> s.bytesDownloaded).reversed());
            }
            List<PeerSession> selected = winners.stream().limit(common.numberOfPreferredNeighbors).collect(Collectors.toList());
            preferredNeighbors.clear();
            preferredNeighbors.addAll(selected.stream().map(s -> s.peerId).collect(Collectors.toSet()));
            for (PeerSession s : sessions.values()) {
                boolean shouldUnchoke = preferredNeighbors.contains(s.peerId) || (optimisticNeighbor != null && optimisticNeighbor.equals(s.peerId));
                applyChokeState(s, !shouldUnchoke);
                s.bytesDownloaded = 0;
            }
            logger.changedPreferredNeighbors(new ArrayList<>(preferredNeighbors));
        }
    }

    /*
     * This method selects a peer session as the optimistic neighbor from the available candidates.
     * If there are no candidates, the optimistic neighbor is set to null.
     * If candidates are available, one is randomly selected and set as the optimistic neighbor,
     * and the choke state is applied accordingly.
     */
    private void handleOptimisticTick() {
        synchronized (stateLock) {
            List<PeerSession> candidates = sessions.values().stream()
                    .filter(s -> s.remoteInterested && s.weChokeRemote && !s.disconnected && !preferredNeighbors.contains(s.peerId))
                    .collect(Collectors.toList());
            if (candidates.isEmpty()) {
                optimisticNeighbor = null;
                return;
            }
            Collections.shuffle(candidates, random);
            PeerSession pick = candidates.get(0);
            optimisticNeighbor = pick.peerId;
            applyChokeState(pick, false);
            logger.changedOptimisticNeighbor(pick.peerId);
        }
    }

    private void applyChokeState(PeerSession session, boolean choke) {
        synchronized (session) {
            if (session.weChokeRemote == choke) return;
            session.weChokeRemote = choke;
        }
        session.send(Message.of(choke ? MessageType.CHOKE : MessageType.UNCHOKE, null));
    }

    private void maybeMarkRemoteComplete(PeerSession session) {
        boolean complete;
        synchronized (session) {
            complete = session.remoteBitfield.countHave() == pm.getPieceCount();
            if (complete) {
                if (!session.remoteComplete) {
                    session.remoteComplete = true;
                    logger.remoteCompleted(session.peerId);
                }
            }
        }
        if (complete) {
            completedPeers.add(session.peerId);
            checkForCompletion();
        }
    }

    private void checkForCompletion() {
        if (!pm.isComplete()) return;
        if (completedPeers.size() == info.all().size()) {
            finishAll();
        }
    }

    private void finishAll() {
        synchronized (this) {
            if (shutdown) return;
            shutdown = true;
        }
        logger.terminated();
        completionLatch.countDown();
        stop();
    }

    public void awaitCompletion() throws InterruptedException {
        completionLatch.await();
    }

    public void stop() {
        shutdown = true;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        acceptor.shutdownNow();
        inboundExecutor.shutdownNow();
        chokeManager.stop();
        for (PeerSession session : sessions.values()) {
            session.close();
        }
        completionLatch.countDown();
    }

    private int performServerHandshake(Socket socket) throws IOException {
        byte[] buf = new byte[Handshake.HEADER.length + Handshake.ZERO_BYTES + 4];
        InputStream in = socket.getInputStream();
        int off = 0;
        while (off < buf.length) {
            int read = in.read(buf, off, buf.length - off);
            if (read < 0) throw new IOException("EOF during handshake");
            off += read;
        }
        Handshake incoming = Handshake.parse(buf);
        Handshake response = new Handshake(selfId);
        socket.getOutputStream().write(response.toBytes());
        socket.getOutputStream().flush();
        return incoming.peerId;
    }

    private int performClientHandshake(Socket socket) throws IOException {
        Handshake outgoing = new Handshake(selfId);
        socket.getOutputStream().write(outgoing.toBytes());
        socket.getOutputStream().flush();
        byte[] buf = new byte[Handshake.HEADER.length + Handshake.ZERO_BYTES + 4];
        InputStream in = socket.getInputStream();
        int off = 0;
        while (off < buf.length) {
            int read = in.read(buf, off, buf.length - off);
            if (read < 0) throw new IOException("EOF during handshake");
            off += read;
        }
        Handshake incoming = Handshake.parse(buf);
        return incoming.peerId;
    }

    private class PeerSession {
        final int peerId;
        final PeerRow row;
        PeerConnection connection;
        Bitfield remoteBitfield = new Bitfield(pm.getPieceCount(), false);
        boolean remoteChokesUs = true;
        boolean weChokeRemote = true;
        boolean remoteInterested = false;
        boolean weAreInterested = false;
        boolean disconnected = false;
        Integer pendingRequest = null;
        long bytesDownloaded = 0;
        boolean remoteComplete = false;

        PeerSession(PeerRow row) {
            this.peerId = row.peerId;
            this.row = row;
        }

        void send(Message message) {
            PeerConnection pc;
            synchronized (this) {
                pc = connection;
            }
            if (pc != null) {
                pc.send(message);
            }
        }

        void close() {
            PeerConnection pc;
            synchronized (this) {
                pc = connection;
            }
            if (pc != null) {
                try { pc.close(); } catch (Exception ignored) {}
            }
        }
    }
}
