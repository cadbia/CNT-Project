## Contribution Notes

1. Caden
— did work on the protocol flows
- fixed ConnectionManage to facilitate the client and server handshakes
- initial bitfield broadcast
- made sure that peers would react with "INTERESTED" or "NOT INTERESTED" when they were supposed to
- tuned preferred and optimistic choke scheduling so logs line up with `Common.cfg`
  
2. Carl
— worked on storage piece scheduling
- built PieceManager
- made sure the bitfield state was in sync with the filesystem
- handled outstanding piece requests to avoid any duplicates
- made sure that the "HAVE" messages are correct
- sanity-checked multi-peer downloads to confirm pieces rotate evenly

3. Dimitri
- worked with logging
- implemented PeerLogger
- made lifecycle hooks so the logs will show all events as intended
- had run.sh spin up peers, making sure they had clean directories and consistent arguments
- worked on the sample log segments for the demo

4. Mia
- worked on configuration and managing the code
- managed Common.cfg and PeerInfo.cfg
- kept all port and ID vals in sync with what was needed for dedmo
- wrote the README.md, and created outline for the demo script
- ran accuaracy checks in the code after any major change
- recorded the terminal evidence

What we did together:
- paired on choke/unchoke timing until the intervals matched the spec
- insured push/pull requests were in 
- practiced the script for the demo
- added additional details to script for the video demo
