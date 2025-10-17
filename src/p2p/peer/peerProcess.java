package p2p.peer;

import p2p.common.*;
import p2p.net.*;

import java.net.Socket;
import java.nio.file.*;
import java.util.*;

public final class peerProcess {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: java peerProcess <peerId>");
      System.exit(1);
    }

    int selfId = Integer.parseInt(args[0]);
    var common = CommonConfig.load(Paths.get("Common.cfg"));
    var table  = PeerTable.load(Paths.get("PeerInfo.cfg"));
    var self   = table.byId(selfId).orElseThrow(() ->
        new IllegalArgumentException("Peer ID " + selfId + " not found in PeerInfo.cfg"));

    System.out.println("Booted peer " + selfId + " on " + self.host() + ":" + self.port()
        + " with " + common.numPieces + " pieces.");

    // --- Start a server socket to accept connections from later peers ---
    new Thread(() -> {
      try (var ss = new java.net.ServerSocket(self.port())) {
        while (true) {
          Socket s = ss.accept();
          var nc = new NeighborConnection(s, selfId);
          nc.startAsAcceptor(); // handshake: read remote id, send self id
        }
      } catch (Exception e) {
        System.err.println("Server for peer " + selfId + " died: " + e.getMessage());
      }
    }, "server-" + selfId).start();

    // --- Dial and connect to earlier peers listed in PeerInfo.cfg ---
    for (var p : table.earlierThan(selfId)) {
      try {
        var s = connectWithRetry(p.host(), p.port(), 10, 300);
        var nc = new NeighborConnection(s, selfId);
        nc.startAsDialer(p.id()); // handshake: send self id, expect remote id = p.id()
        // Send a trivial message just to test message flow
        nc.send(new Message(Message.Type.NOT_INTERESTED, new byte[0]));
      } catch (Exception e) {
        System.err.println("Dial to peer " + p.id() + " failed: " + e.getMessage());
      }
    }
  }

  // --- Helper: retries connection to avoid race conditions ---
  private static Socket connectWithRetry(String host, int port, int attempts, long sleepMs)
      throws Exception {
    Exception last = null;
    for (int i = 0; i < attempts; i++) {
      try {
        return new Socket(host, port);
      } catch (Exception e) {
        last = e;
        Thread.sleep(sleepMs);
      }
    }
    throw last;
  }
}