package p2p.net;
import java.io.*; import java.nio.charset.StandardCharsets;

public final class Handshake {
  private static final byte[] HEADER =
      "P2PFILESHARINGPROJ".getBytes(StandardCharsets.US_ASCII); // 18 bytes

  // Send YOUR (the sender's) id
  public static void send(DataOutputStream out, int selfPeerId) throws IOException {
    out.write(HEADER);
    out.write(new byte[10]);           // 10 zero bytes
    out.writeInt(selfPeerId);          // <-- sender's id
    out.flush();
  }

  // Receive and validate header, return REMOTE (sender's) id
  public static int recv(DataInputStream in) throws IOException {
    byte[] hdr = in.readNBytes(18);
    if (hdr.length != 18 || !java.util.Arrays.equals(hdr, HEADER)) {
      throw new IOException("Bad header");
    }
    in.readNBytes(10);                 // skip ten 0s
    return in.readInt();               // <-- remote peer id (sender)
  }
}