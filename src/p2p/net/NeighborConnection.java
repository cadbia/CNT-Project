package p2p.net;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public final class NeighborConnection {
  private final int selfPeerId;
  private volatile int remotePeerId;
  private final Socket socket;
  private final DataInputStream in;
  private final DataOutputStream out;
  private final BlockingQueue<Message> sendQ = new LinkedBlockingQueue<>();
  private volatile boolean running = true;

  public NeighborConnection(Socket s, int selfPeerId) throws IOException {
    this.socket = s; this.selfPeerId = selfPeerId;
    this.in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
    this.out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
  }

  // expectedRemoteId is known for dialer (e.g., 1001). Use -1 to skip checking.
  public void startAsDialer(int expectedRemoteId) {
    new Thread(() -> {
      try {
        Handshake.send(out, selfPeerId);                 // send MY id
        int got = Handshake.recv(in);                    // read REMOTE id
        if (expectedRemoteId != -1 && got != expectedRemoteId) {
          throw new IOException("Unexpected remote id: " + got);
        }
        this.remotePeerId = got;
        System.out.println("Connected (dial) to " + got);
        new Thread(this::writerLoop, "writer-"+got).start();
        readerLoop();
      } catch (IOException e) { shutdown("dialer err: "+e.getMessage()); }
    }, "dialer-hs").start();
  }

  public void startAsAcceptor() {
    new Thread(() -> {
      try {
        int got = Handshake.recv(in);                    // read REMOTE id first
        this.remotePeerId = got;
        Handshake.send(out, selfPeerId);                 // send MY id back
        System.out.println("Connected (accept) from " + got);
        new Thread(this::writerLoop, "writer-"+got).start();
        readerLoop();
      } catch (IOException e) { shutdown("acceptor err: "+e.getMessage()); }
    }, "acceptor-hs").start();
  }

  public void send(Message m) { if (running) sendQ.offer(m); }

  private void writerLoop() {
    try {
      while (running) MessageCodec.write(out, sendQ.take());
    } catch (Exception ignore) { } finally { shutdown("writer exit"); }
  }

  private void readerLoop() {
    try {
      while (running) {
        Message m = MessageCodec.read(in);
        System.out.println("Got "+m.type+" ("+m.payload.length+"B) from "+remotePeerId);
      }
    } catch (IOException ignore) { } finally { shutdown("reader exit"); }
  }

  private synchronized void shutdown(String why) {
    if (!running) return;
    running = false;
    try { socket.close(); } catch (IOException ignore) {}
    System.out.println("Conn "+remotePeerId+" closed: "+why);
  }
}