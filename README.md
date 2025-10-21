# Project midpoint check

made for the project midpoint check. 
Reqs: at least 500 lines and compileable.

## To build
```bash
javac -d out $(find src -name '*.java')
```

## To Run (example)
```bash
# Terminal 1
java -cp out peer.PeerProcess 1001
# Terminal 2
java -cp out peer.PeerProcess 1002
# Terminal 3
java -cp out peer.PeerProcess 1003
```

Update `resources/PeerInfo.cfg` and `resources/Common.cfg` to match your environment.
