#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if [[ $# -lt 1 ]]; then
	echo "Usage: ./run.sh <peerID> [peerID ...]" >&2
	echo "Optional: export CONFIG_DIR=/path/to/config" >&2
	exit 1
fi

mkdir -p out
javac -d out $(find src -name '*.java')

CONFIG_DIR=${CONFIG_DIR:-resources}

for peer_id in "$@"; do
	(
		echo "Starting peer $peer_id using config dir '$CONFIG_DIR'"
		java -cp out peer.PeerProcess "$peer_id" "$CONFIG_DIR"
	) &
done

wait
