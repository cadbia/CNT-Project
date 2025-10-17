#!/usr/bin/env bash
set -euo pipefail
javac -d out $(find src -name '*.java')
java -cp out peer.PeerProcess "${1:-1001}"
