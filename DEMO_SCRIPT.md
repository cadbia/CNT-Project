# Demo Script & Evidence Checklist

Use this cue sheet while recording the 5–8 minute grading video. Each section maps directly to the rubric so the reviewers can check boxes in real time.

# Demo Script & Evidence Checklist

Use this cue sheet while recording the 5–8 minute grading video. Each section maps directly to the rubric so reviewers can check boxes in real time.

## Cast & Roles

- **Caden B** – Host & overview / completion narrator
- **Carl S** – Config + interest/choke walkthrough
- **Dimitri D** – Code deep dive + log proof for messaging
- **Mia B** – Terminal driver + file-exchange evidence

## Shot List With Exact Actions

| Time | Speaker | Action | Spoken Script | Commands / Files | Rubric |
| --- | --- | --- | --- | --- | --- |
| 0:00–0:30 | **Caden** | Title card (slides) | “Welcome to CNT4007 Project Group. We’ll show configs, handshakes, choke logic, file transfers, and shutdown to satisfy the rubric.” | — | Overview |
| 0:30–1:05 | **Carl** | Screen share `resources/Common.cfg` | “`NumberOfPreferredNeighbors=2`, `UnchokingInterval=5`, `OptimisticUnchokingInterval=15`, file `sample.data` size 20 MB.” | `resources/Common.cfg` | 1a |
| 1:05–1:30 | **Carl** | Switch to `resources/PeerInfo.cfg` | “Peer 1001 seeds (`hasFile 1`), peers 1002/1003 start empty. Ports 6001–6003.” | `resources/PeerInfo.cfg` | 1a |
| 1:30–2:05 | **Dimitri** | VS Code on `ConnectionManager.java` | “Here `performClientHandshake` writes the header and waits for the peer ID before any other message. `sendBitfield` fires immediately after registration.” | `src/peer/net/ConnectionManager.java` | 2a, 2b |
| 2:05–2:40 | **Mia** | Terminal build/run | Say: “Compiling fresh and launching peers 1001–1003.” Run:
   ```bash
   rm -rf out && javac -d out $(find src -name '*.java')
   ./run.sh 1001 1002 1003
   ```
Keep terminal visible as logs stream. | Terminal window | 1b, 1c |
| 2:40–3:10 | **Caden** | Open `peer_1001/log_peer_1001.log` | “Line 1: ‘Peer 1001 started…’. Next lines: ‘is connected from Peer 1002/1003’. Shows later peers dial earlier ones.” | `peer_1001/log_peer_1001.log` | 1b |
| 3:10–3:40 | **Carl** | Search logs for interest | Use VS Code search `"received the 'interested'"` and `"'not interested'"`. Narrate: “Peers 1002/1003 become interested right after bitfield; they switch to not interested once complete.” | logs | 2c |
| 3:40–4:10 | **Dimitri** | Highlight choke logs | Search `"is unchoked"`, `"has the preferred neighbors"`, `"has the optimistically unchoked neighbor"`. “Preferred neighbors rotate every 5s; optimistic slot flips every 15s as configured.” (If needed, mention config tweak to trigger optimistic entry.) | logs | 2d, 2e |
| 4:10–4:50 | **Mia** | Showcase file exchange in `peer_1002/log_peer_1002.log` | Scroll showing repeating pattern: `is unchoked by 1001`, `has downloaded the piece …`, `received the 'have' message …`. Mention these cover REQUEST, PIECE, HAVE, bitfield update. | `peer_1002/log_peer_1002.log` | 3a–3f |
| 4:50–5:20 | **Caden** | Completion proof | Show tail of each log with `has downloaded the complete file`, `confirms Peer …`, `terminates because all peers now have the complete file.` Mention terminal prompt returned automatically. | logs + terminal | 1c, 4 |
| 5:20–5:50 | **All** | Split summary | Each member (10s each) states: role + major component built + tests run. Point viewers to `README_CONTRIBUTIONS.md`. | README files | Policy |

## Detailed Cue Cards

### 0. Prep (off camera)
1. **Caden** runs `rm -rf peer_1001 peer_1002 peer_1003` if a clean state is desired.
2. **Carl** ensures `resources/Common.cfg` and `PeerInfo.cfg` have the values cited above.
3. **Mia** recreates the seed file if missing:
    ```bash
    python3 tools/create_seed.py   # or the inline snippet from README
    ```

### 1. Config Segment (Carl)
- Zoom to 125% so text is readable.
- Lines to read verbatim:
   - “NumberOfPreferredNeighbors equals 2 so we unchoke two neighbors every cycle.”
   - “Optimistic interval is 15 seconds to match the spec.”
   - “Peer 1001 is the only row with `hasFile 1`, so it seeds the swarm.”

### 2. Handshake/Bitfield Segment (Dimitri)
- Highlight `performClientHandshake` block and say:
   > “Lines XX–YY write the handshake header `P2PFILESHARINGPROJ`, send our peer ID, then wait for the remote ID before instantiating `PeerConnection`.”
- Scroll to `sendBitfield(session);` and say:
   > “Immediately after registering the connection we call `sendBitfield`, so neighbors know which pieces we have before sending INTERESTED/NOT_INTERESTED.”

### 3. Terminal Segment (Mia)
- Commands to run live:
   ```bash
   rm -rf out
   javac -d out $(find src -name '*.java')
   ./run.sh 1001 1002 1003
   ```
- While output scrolls, narrate: “Each peer creates its own `peer_<id>` directory and starts logging immediately; we’ll inspect those next.”

### 4. Log Evidence (Caden & Carl & Dimitri)
- **Caden**: Use VS Code preview of `peer_1001/log_peer_1001.log`. Use `Cmd+G` for lines showing `connected from`.
- **Carl**: Use search panel for `interested` / `not interested` terms. Mention timestamps (e.g., `[2025-11-20 17:48:26]`).
- **Dimitri**: Search for `unchoked`, `preferred neighbors`, `optimistically unchoked neighbor`. If the optimistic line doesn’t exist, stop and explain the config tweak (set `NumberOfPreferredNeighbors 1`) and optionally rerun to capture it.

### 5. File Exchange (Mia)
- Tail `peer_1002/log_peer_1002.log` via terminal or editor.
- Read a block aloud:
   > “`[17:48:31] is unchoked by 1001` → `has downloaded the piece 468` → `received the 'have' message from 1003 for the piece 679`. That trio proves REQUEST, PIECE, HAVE propagation, and remote bitfield update.”

### 6. Completion & Shutdown (Caden)
- Use `tail -n 20 peer_100*/log_peer_*.log` to show the end-of-run lines.
- Explicitly say: “All peers detect completion, log confirmations for every neighbor, and `run.sh` returns us to a shell prompt without manual kills.”

### 7. Wrap-Up (All)
- Display `README.md` (demo table) and `README_CONTRIBUTIONS.md`.
- Each member states: “I’m <name>; I implemented … and validated it by …”.
- Caden closes with: “Full instructions and the video link live in README. That concludes our demonstration.”

## Final Reminders

- Keep zoom at readable levels (≥125%).
- If optimistic unchoke logs don’t appear, pause the recording, change `NumberOfPreferredNeighbors` to 1, rerun `./run.sh 1001 1002 1003`, and resume once the log line exists.
- Mention any limitations verbally and update README accordingly for partial credit.

Following this cue sheet ensures every rubric bullet is explicitly demonstrated, with named presenters and exact commands.
