package peer.util;

public class ByteUtils {
    public static void putInt(byte[] arr, int offset, int value) {
        arr[offset]     = (byte)((value >>> 24) & 0xFF);
        arr[offset + 1] = (byte)((value >>> 16) & 0xFF);
        arr[offset + 2] = (byte)((value >>> 8) & 0xFF);
        arr[offset + 3] = (byte)(value & 0xFF);
    }
    public static int getInt(byte[] arr, int offset) {
        return ((arr[offset] & 0xFF) << 24) |
               ((arr[offset+1] & 0xFF) << 16) |
               ((arr[offset+2] & 0xFF) << 8)  |
               (arr[offset+3] & 0xFF);
    }
    public static boolean hasBit(byte[] bits, int idx) {
        int byteIdx = idx / 8;
        int bitIdx = 7 - (idx % 8);
        if (byteIdx >= bits.length) return false;
        return (bits[byteIdx] & (1 << bitIdx)) != 0;
    }
    public static void setBit(byte[] bits, int idx) {
        int byteIdx = idx / 8;
        int bitIdx = 7 - (idx % 8);
        bits[byteIdx] = (byte)(bits[byteIdx] | (1 << bitIdx));
    }
}
