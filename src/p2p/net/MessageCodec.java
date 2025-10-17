package p2p.net;
import java.io.*;
public final class MessageCodec {
  public static void write(DataOutputStream out, Message m) throws IOException{
    int len = 1 + (m.payload==null?0:m.payload.length);
    out.writeInt(len); out.writeByte(m.type.code);
    if (m.payload!=null) out.write(m.payload); out.flush();
  }
  public static Message read(DataInputStream in) throws IOException{
    int len = in.readInt(); int type = in.readUnsignedByte();
    byte[] payload = (len>1)? in.readNBytes(len-1) : new byte[0];
    return new Message(Message.Type.from(type), payload);
  }
}
