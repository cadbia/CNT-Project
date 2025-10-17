# CNT4007 P2P — Midpoint Skeleton

This is a **compilable** midpoint skeleton that sets up the project structure, config loaders, protocol primitives, logging, core state,
and networking scaffolding. It compiles cleanly and can be extended to full functionality.

## Build
```bash
javac -d out $(find src -name '*.java')
```

## Run (example)
```bash
# Terminal 1
java -cp out peer.PeerProcess 1001
# Terminal 2
java -cp out peer.PeerProcess 1002
# Terminal 3
java -cp out peer.PeerProcess 1003
```

Update `resources/PeerInfo.cfg` and `resources/Common.cfg` to match your environment.
