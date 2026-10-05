# SpyCraft filming director

A development-only local file API for staging and recording a real SpyCraft player.
It is dormant unless the client is launched with `-Dspycraft.director=true`.
ReplayMod is optional for normal gameplay and required for `take`/`export_replay`.
The initial integration targets ReplayMod **1.21-2.6.27** on Minecraft **1.21.1**.

## Launch

Build SpyCraft with Java 21 and `./gradlew build`. Use a separate Prism instance
with the built SpyCraft JAR, Fabric API, and ReplayMod. Do not copy your existing
worlds into the filming instance. Set its Java arguments to:

```
-Dspycraft.director=true -Dspycraft.director.ffmpeg=/opt/homebrew/bin/ffmpeg
```

Use the path to your FFmpeg executable on other systems. The director writes
`spycraft-director/ready.json` inside the instance's Minecraft folder.
There is no HTTP listener, new port, or MCP dependency. This API can later be
wrapped by an MCP server without changing how the scene runs.

## Record a pilot

Run the CLI from the repository root, passing the filming instance's Minecraft
folder as `--game-dir` on every command:

```
python3 spycraft/tools/director/director.py --game-dir /path/to/minecraft create_world
python3 spycraft/tools/director/director.py --game-dir /path/to/minecraft prepare
python3 spycraft/tools/director/director.py --game-dir /path/to/minecraft take
python3 spycraft/tools/director/director.py --game-dir /path/to/minecraft status --save /path/to/take.json
python3 spycraft/tools/director/director.py --game-dir /path/to/minecraft disconnect
python3 spycraft/tools/director/director.py --game-dir /path/to/minecraft --timeout 600 export_replay --replay /path/to/minecraft/replay_recordings/recording.mcpr --take /path/to/take.json
```

`create_world` starts loading a fresh superflat `SPYCRAFT_FILM_<request-id>` world;
wait until the player has loaded before preparing it. `prepare` builds the set
and equips the actor. `take` requires ReplayMod to be recording. The 15-second
take idles on the roof for three seconds, moves the actor just beyond its edge,
and lets the real backpack deploy and steer the descent. The director releases
movement keys and fixes the player's look direction during the take.

Poll `status` until `complete`, `deployed`, and `landed` are all true, then save
that response. It includes server-tick position samples and the recording's
start time. Disconnect closes the world connection before saving. Wait for the
`.mcpr` to appear in `replay_recordings` before exporting.

Export writes a named camera timeline back into the replay and renders a silent
15-second **1280×720, 30 fps H.264 MP4** inside `spycraft-director`. The camera
moves from an establishing angle to a side follow shot. Audio, titles, and final
editing are separate from ReplayMod rendering. Use `screenshot` to save a PNG of
the current game frame for diagnostics.

## Limits and recovery

Scene actions only operate in a singleplayer world whose name starts with
`SPYCRAFT_FILM_`. Preparation replaces blocks in the film set (x −40…40,
y 79…99, z −20…85), equips the player, and changes that world's time/weather.
Use disposable filming worlds. It does not alter a normal world or connect to
a multiplayer server. Export only accepts recordings in this instance.

Send commands one at a time. Requests and responses are JSON files, written
atomically. The CLI serializes writers. A timeout does not cancel an action:
check the printed response path before retrying. If a CLI process is killed,
remove its stale `writer.lock` only after verifying it is no longer running.
ReplayMod may offer recording recovery after a forced game shutdown; keep the
partial recording until recovery completes.

The integration uses reflection to avoid making ReplayMod a mandatory mod
dependency. Other versions may change these APIs. A successful server take does
not establish visual replay compatibility: inspect exported frames, particularly
particles and any client-only custom models, before using footage in a trailer.
