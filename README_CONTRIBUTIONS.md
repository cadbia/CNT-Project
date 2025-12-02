# CNT4007 P2P — Contribution Notes

## Caden B — Networking & Protocol Flow
- rewrote `ConnectionManager` to manage client/server handshakes cleanly
- wired the initial bitfield broadcast and verified peers react with INTERESTED/NOT INTERESTED as expected
- tuned preferred and optimistic choke scheduling so logs line up with `Common.cfg`

## Carl S — Storage & Piece Scheduling
- built the disk-backed `PieceManager` and keeps the bitfield state in sync with the filesystem
- manages outstanding piece requests to avoid duplicates and makes sure HAVE messages fan out correctly
- sanity-checked multi-peer downloads to confirm pieces rotate evenly

## Dimitri D — Logging & Automation
- implemented `PeerLogger` and the lifecycle hooks so every rubric event shows up in the logs
- scripted `run.sh` to spin up peers with clean directories and consistent arguments
- pulled sample log segments for the video so graders can follow along without guessing

## Mia B — Configs, Docs, and QA
- owns `Common.cfg`/`PeerInfo.cfg`, keeps port/ID values in sync with what we demo
- authored `README.md`, `DEMO_SCRIPT.md`, and this contributions note in plain language
- ran full-system smoke tests after each major change and recorded the terminal evidence we cite

## Teamwide Work
- paired on choke/unchoke timing until the intervals matched the spec
- rotated code reviews before merging anything into `master`
- rehearsed and recorded the 5–8 minute walkthrough outlined in `README.md`