package peer.net;

import peer.log.PeerLogger;
import peer.protocol.Handshake;
import peer.protocol.Message;
import peer.protocol.MessageType;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PeerConnection implements Closeable {
    public final int remotePeerId;
    private final Socket socket;
    private final PeerLogger logger;
    private final BlockingQueue<Message> outbound = new LinkedBlockingQueue<>();
    private Thread reader;
    private Thread writer;

    public PeerConnection(int remotePeerId, Socket socket, PeerLogger logger) throws IOException {
        this.remotePeerId = remotePeerId;
        this.socket = socket;
        this.logger = logger;
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
                    // Midpoint: simply log receipt; ConnectionManager will not dispatch handlers yet.
                    // In a fuller design, you'd push to a per-connection queue/callback.
                }
            } catch (Exception ignored) {}
        }, "reader-" + remotePeerId);
        writer = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Message m = outbound.take();
                    out.write(m.toBytes());
                    out.flush();
                }
            } catch (Exception ignored) {}
        }, "writer-" + remotePeerId);

        reader.setDaemon(true);
        writer.setDaemon(true);
        reader.start();
        writer.start();
    }

    public void sendHandshake(int selfId) throws IOException {
        Handshake hs = new Handshake(selfId);
        socket.getOutputStream().write(hs.toBytes());
        socket.getOutputStream().flush();
    }

    public int recvHandshake() throws IOException {
        byte[] buf = new byte[Handshake.HEADER.length + Handshake.ZERO_BYTES + 4];
        int off = 0;
        InputStream in = socket.getInputStream();
        while (off < buf.length) {
            int r = in.read(buf, off, buf.length - off);
            if (r < 0) throw new IOException("EOF during handshake");
            off += r;
        }
        Handshake hs = Handshake.parse(buf);
        return hs.peerId;
    }

    public void send(Message m) {
        outbound.offer(m);
    }

    @Override
    public void close() throws IOException {
        try { if (reader != null) reader.interrupt(); } catch (Exception ignored) {}
        try { if (writer != null) writer.interrupt(); } catch (Exception ignored) {}
        socket.close();
    }
}
