# Playing Together Over Home Wi-Fi (LAN Multiplayer)

Once you compile your mod, you and Erik can play together in the same world on your home local network!

---

## 1. Exporting Your Mod `.jar`

In `~/Develop/mc/spycraft`, run:
```bash
./gradlew build
```
This compiles your code and creates:
```
spycraft/build/libs/spycraft-1.0.0.jar
```
This single `.jar` file is the mod package.

---

## 2. Setting Up Prism Launcher (Already Installed!)

You already have **Prism Launcher** installed at `/Applications/Prism Launcher.app`! Prism Launcher is by far the cleanest launcher for managing Fabric mods and instances.

### Steps on Dad's Computer & Erik's Computer / Steam Deck:
1. Open **Prism Launcher**.
2. Click **Add Instance**:
   - Version: `1.21.1`
   - Mod Loader: Select **Fabric** (latest version)
   - Click **OK**.
3. In the instance menu, click **Mods** on the left sidebar:
   - Click **Download Mods** -> search and add **Fabric API** (required by all Fabric mods).
   - Click **Add .jar** -> select your built `spycraft/build/libs/spycraft-1.0.0.jar` (or search for `SpyCraft` on Modrinth!).
4. Launch the instance!

---

## 3. Connecting Together via LAN (Zero Configuration)

Minecraft has a built-in LAN multiplayer feature:

1. **Host the game (Dad or Erik)**:
   - Start a Singleplayer world.
   - Press `Esc` -> click **Open to LAN**.
   - Choose game mode (Creative or Survival) and click **Start LAN World**.
   - Minecraft will show a message in chat: `Local game hosted on port XXXXX`.
2. **Join the game (The other player)**:
   - On the second computer (connected to the same home Wi-Fi), launch the same Minecraft version with the mod installed.
   - Click **Multiplayer**.
   - Scroll to the bottom of the list — the LAN world will automatically appear under **LAN Worlds**!
   - Click **Join Server**.

> [!TIP]
> Both players must have `spycraft-1.0.0.jar` and `Fabric API` in their `mods` folder so that custom items, textures, and effects sync properly between clients.
