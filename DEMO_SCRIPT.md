# Demo Script & Evidence Checklist

Use this script while recording the grading video to prove every rubric item. The demo should last 5–8 minutes and follow the flow below.

## 0. Prep (off camera or at the start)
1. Ensure `resources/Common.cfg` and `resources/PeerInfo.cfg` reflect the network you will show (one peer starts with the entire file, the rest do not).
2. Clean previous state: remove `peer_*` folders if you want a fresh run.
3. Export any alternate config directory path if needed, e.g. `export CONFIG_DIR=resources`.

## 1. Start the peers (35%)
1. **Show configs**: briefly open both config files and mention key values (preferred neighbor count, ports, which peer owns the file).
2. **Compile & launch**: run `./run.sh 1001 1002 1003` (or the IDs from your config). Keep the terminal visible.
3. **Narrate startup logs**: for each `peer_<id>/log_peer_<id>.log`, point to the early lines:
   - “Peer X started. Config dir=… pieces=…”
   - Connections established (`make a connection to` / `is connected from`). Highlight that later peers connect to earlier ones.
4. **Termination condition**: explain that each peer auto-stops only after all peers have the full file (will be proven at the end).

## 2. After connection requirements (30%)
1. **Handshake**: mention that `ConnectionManager` exchanges handshakes before other traffic. Show a short snippet from the code or log statements around connection time.
2. **Bitfield exchange**: scroll to the moment when `BITFIELD` messages are sent (first entries after connection) or show the code path if logs are too long.
3. **Interested/Not Interested**: highlight log lines `received the 'interested'` and `received the 'not interested'` for at least one pair of peers.
4. **Choke/Unchoke**: show lines `is choked by …` / `is unchoked by …` that appear every `UnchokingInterval`. Mention the configured interval.
5. **Preferred/Optimistic neighbors**: point to `has the preferred neighbors […]` and `has the optimistically unchoked neighbor …` entries to prove the scheduler is working.

## 3. File exchange requirements (30%)
1. **Request/Piece**: locate log lines showing `has downloaded the piece …` to prove request/response occurred (optionally tail the log live during the run).
2. **Have propagation**: show `received the 'have' message from …` lines after each download.
3. **Interested/Not Interested again**: when a peer finishes, show that others log `received the 'not interested' message …` once they no longer need pieces.
4. **Bitfield updates**: explain that the `HAVE` processing updates remote bitfields, referencing the code or showing the log progression of owned piece counts.

## 4. Stop service correctly (5%)
1. Wait for the run to finish; each peer prints `has downloaded the complete file.` followed by `confirms Peer … has downloaded the complete file.` for others.
2. Finally, highlight the `terminates because all peers now have the complete file.` log entry and the fact that the peer processes exit automatically (the `run.sh` terminal prompt returns).

## 5. Wrap-up & README callouts
1. Point the graders to `README.md` for:
   - group member list & contribution notes
   - video link (if hosted externally)
   - instructions for rerunning the demo themselves
2. Mention any partial-credit caveats if something still isn’t perfect.

Following this script ensures every rubric bullet is demonstrated explicitly, maximizing the likelihood of full credit.
