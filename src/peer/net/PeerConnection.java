package peer.net;

import peer.log.PeerLogger;
import peer.net.MessageHandler;
import peer.protocol.Message;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerConnection implements Closeable {
    public final int remotePeerId;
    private final Socket socket;
    private final PeerLogger logger;
    private final MessageHandler handler;
    private final BlockingQueue<Message> outbound = new LinkedBlockingQueue<>();
    private Thread reader;
    private Thread writer;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public PeerConnection(int remotePeerId, Socket socket, PeerLogger logger, MessageHandler handler) throws IOException {
        this.remotePeerId = remotePeerId;
        this.socket = socket;
        this.logger = logger;
        this.handler = handler;
        startThreads();
    }

    private void startThreads() throws IOException {
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        reader = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Message m = Message.parse(in);
                    if (m == null) break;
                    handler.onMessage(remotePeerId, m);
                }
            } catch (Exception ex) {
                logger.info("reader for " + remotePeerId + " closed: " + ex.getMessage());
            } finally {
                handler.onDisconnect(remotePeerId);
            }
        }, "reader-" + remotePeerId);
        writer = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Message m = outbound.take();
                    out.write(m.toBytes());
                    out.flush();
                }
            } catch (Exception ex) {
                logger.info("writer for " + remotePeerId + " closed: " + ex.getMessage());
            }
        }, "writer-" + remotePeerId);

        reader.setDaemon(true);
        writer.setDaemon(true);
        reader.start();
        writer.start();
    }

    public void send(Message m) {
        if (!closed.get()) {
            outbound.offer(m);
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true)) return;
        try { if (reader != null) reader.interrupt(); } catch (Exception ignored) {}
        try { if (writer != null) writer.interrupt(); } catch (Exception ignored) {}
        socket.close();
    }
}
