package peer.core;

import peer.log.PeerLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChokeManager {
    private final int selfId;
    private final int k;
    private final int unchokeSec;
    private final int optSec;
    private final PeerLogger logger;
    private final Random rnd = new Random();

    private final Set<Integer> interested = ConcurrentHashMap.newKeySet();
    private final Set<Integer> unchoked = ConcurrentHashMap.newKeySet();

    private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(2);

    public ChokeManager(int selfId, int k, int unchokeSec, int optSec, PeerLogger logger) {
        this.selfId = selfId;
        this.k = k;
        this.unchokeSec = unchokeSec;
        this.optSec = optSec;
        this.logger = logger;
    }

    public void start() {
        ses.scheduleAtFixedRate(this::recomputePreferred, unchokeSec, unchokeSec, TimeUnit.SECONDS);
        ses.scheduleAtFixedRate(this::optimistic, optSec, optSec, TimeUnit.SECONDS);
    }

    public void stop() { ses.shutdownNow(); }

    public void setInterested(int peerId, boolean isInterested) {
        if (isInterested) interested.add(peerId); else interested.remove(peerId);
    }

    private void recomputePreferred() {
        List<Integer> ints = new ArrayList<>(interested);
        List<Integer> chosen = new ArrayList<>();
        while (chosen.size() < k && !ints.isEmpty()) {
            int idx = rnd.nextInt(ints.size());
            chosen.add(ints.remove(idx));
        }
        unchoked.clear();
        unchoked.addAll(chosen);
        logger.changedPreferredNeighbors(chosen);
        // In full implementation you would send choke/unchoke messages here.
    }

    private void optimistic() {
        List<Integer> ints = new ArrayList<>(interested);
        ints.removeAll(unchoked);
        if (!ints.isEmpty()) {
            int pick = ints.get(rnd.nextInt(ints.size()));
            logger.changedOptimisticNeighbor(pick);
            // In full implementation you would send the unchoke message here.
        }
    }
}
