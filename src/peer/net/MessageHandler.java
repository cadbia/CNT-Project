package peer.net;

import peer.protocol.Message;

/** quick callback that lets PeerConnection stay dumb about higher-level state. */
public interface MessageHandler {
    void onMessage(int remotePeerId, Message message);
    void onDisconnect(int remotePeerId);
}
