package p2p.net;
public final class Message {
  public enum Type { CHOKE(0), UNCHOKE(1), INTERESTED(2), NOT_INTERESTED(3), HAVE(4), BITFIELD(5), REQUEST(6), PIECE(7);
    public final int code; Type(int c){code=c;} public static Type from(int c){ for (var t:values()) if(t.code==c) return t; throw new IllegalArgumentException(); } }
  public final Type type; public final byte[] payload;
  public Message(Type t, byte[] p){ type=t; payload=p==null? new byte[0]:p; }
}