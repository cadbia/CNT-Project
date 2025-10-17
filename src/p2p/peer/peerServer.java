package p2p.peer;

import p2p.net.NeighborConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.BiConsumer;

public final class peerServer implements Runnable {
  private final int port;
  private final BiConsumer<Socket,Integer> onAccept;
  public peerServer(int port, BiConsumer<Socket,Integer> onAccept){
    this.port = port; this.onAccept = onAccept;
  }
  @Override public void run() {
    try (ServerSocket ss = new ServerSocket(port)) {
      while (true) {
        Socket s = ss.accept();
        // caller provides expected remote id (we’ll pass via socket attr or map; for now, assume caller knows)
        onAccept.accept(s, -1);
      }
    } catch (IOException e) {
      System.err.println("Server error: "+e.getMessage());
    }
  }
}
