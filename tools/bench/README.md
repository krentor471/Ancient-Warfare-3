# NPC benchmark

Measures server tick time (MSPT) with many NPCs on a dedicated dev server. 50 ms is the limit for 20 TPS.

Setup, once, in `run/server.properties`:

```
enable-rcon=true
rcon.password=bench
level-name=bench
level-type=minecraft\:flat
```

Then start `./gradlew runServer` and prepare the world over RCON:

```
python3 tools/bench/rcon.py 'setworldspawn 0 -60 0' 'forceload add -64 -64 64 64' \
  'gamerule doMobSpawning false' 'gamerule doDaylightCycle false'
bash tools/bench/suite.sh
```

`bench.py MODE N...` modes: `idle` (faction civilians), `soldiers` (idle faction soldiers),
`battle` (N/2 empire vs N/2 norska soldiers, invulnerable so the fight never ends), `workers`.

Profiling: `jcmd <server pid> JFR.start settings=profile duration=30s filename=out.jfr`, then
`python3 tools/bench/jfrtop.py out.jfr` prints the hottest frames of the server thread.
