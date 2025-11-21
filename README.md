# CNT4007 P2P — Complete Peer

This repository now ships a runnable BitTorrent-style peer featuring:

- disk-backed piece management with HAVE/BITFIELD dissemination
- full choke/unchoke rotation (preferred + optimistic) powered by `ChokeManager`
- callback-driven networking via `PeerConnection`/`MessageHandler`
- graceful lifecycle management that exits once **all** peers report completion

## Build
```bash
javac -d out $(find src -name '*.java')
```

## Quick Run
```bash
# compile & run the specified peers (defaults to resources/)
./run.sh 1001 1002 1003
```

`PeerProcess` accepts an optional second argument that points to a directory containing `Common.cfg` and `PeerInfo.cfg` if you want to run against an alternate config set:

```bash
java -cp out peer.PeerProcess 1001 project_config_file_large
```

## Workflow
1. Update `resources/Common.cfg` with the desired file name, size, and piece size.
2. Update `resources/PeerInfo.cfg` with the full peer table, flags for which peers already have the file, and their listening ports.
3. Build once, then launch each peer (see `run.sh` convenience launcher) on the machines/ports defined in `PeerInfo.cfg`.
4. Each peer logs to `peer_<id>/log_peer_<id>.log` and automatically terminates once every peer reports the complete file.

## Repository Layout

- `src/peer/core/*` — bitfields, choke scheduler, and disk persistence.
- `src/peer/net/*` — socket management, message parsing, and protocol orchestration.
- `src/peer/log/PeerLogger.java` — spec-compliant logging helpers.
- `resources/` — canonical configuration directory read by default.

Feel free to prune/add additional config folders beside `resources/`; pass the directory path as the optional second argument when launching a peer.

## 🎬 Demo Reference

To prove every rubric requirement during grading, follow [`DEMO_SCRIPT.md`](./DEMO_SCRIPT.md). It contains a minute-by-minute checklist covering config verification, connection evidence, choke scheduling, file transfers, and automatic shutdown so your video hits 100% of the points.
