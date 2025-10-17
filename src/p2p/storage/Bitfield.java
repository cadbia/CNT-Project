package p2p.storage;
import java.util.BitSet;
public final class Bitfield {
  private final BitSet bits; private final int size;
  public Bitfield(int numPieces, boolean hasAll){ this.size=numPieces; this.bits=new BitSet(numPieces); if (hasAll) bits.set(0,numPieces); }
  public boolean has(int i){ return bits.get(i); }
  public void set(int i){ bits.set(i); }
  public byte[] toPayload(){
    int nbytes = (size + 7) / 8; byte[] out = new byte[nbytes];
    for (int i=0;i<size;i++){ if(bits.get(i)){ int byteIdx=i/8; int bitIdx=7-(i%8); out[byteIdx] |= (1<<bitIdx); } }
    return out;
  }
  public static Bitfield fromPayload(byte[] payload, int size){
    BitSet bs = new BitSet(size);
    for (int i=0;i<size;i++){ int byteIdx=i/8, bitIdx=7-(i%8); boolean set = (payload[byteIdx] & (1<<bitIdx))!=0; if(set) bs.set(i); }
    Bitfield bf = new Bitfield(size,false); bf.bits.or(bs); return bf;
  }
}