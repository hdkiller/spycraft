# 🎮 Minecraft Modding Environment (`~/Develop/mc`)

Welcome to Minecraft Modding! This workspace is set up for **László & Erik** to explore, code, and play custom Minecraft mods together on macOS.

---

## ⚡ Quickstart: Launch the Game in 1 Command

Open your terminal in `~/Develop/mc/erikcraft` and run:

```bash
cd ~/Develop/mc/erikcraft
./gradlew runClient
```

Minecraft 1.21.1 will boot up with **ErikCraft** already loaded!

> [!TIP]
> **Using Cursor or VS Code?**
> Simply open the folder `~/Develop/mc/erikcraft` in Cursor / VS Code. Press **F5** (or open the *Run & Debug* panel and click **Minecraft Client**) to start Minecraft with full code debugging attached!

---

## 🕵️‍♂️ Erik's Secret Agent & Spy Arsenal

We built a complete spy toolkit inside **`ErikCraft`**, available in the **"ErikCraft Spy & Adventure Gear"** creative tab:

### 1. 🔫 Tactical Grappling Hook Gun ([`GrapplingHookGunItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/GrapplingHookGunItem.java))
- Aim at any wall, roof, or mountain ledge up to 32 blocks away and right-click.
- Shoots a cable with critical-spark particle trails and reels Erik up to the ledge!
- Grants 3 seconds of soft landing (Slow Falling) so Erik doesn't take fall damage when infiltrating.

### 2. 📡 Spy Bug & Radar Tracker ([`MobTrackerItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/MobTrackerItem.java))
- **Mount on Mobs or Players**: Right-click ANY mob (or Dad!) to plant a live tracking bug. The target glows through walls for 10 minutes!
- **Plant GPS Beacon on Blocks**: Right-click ANY block (secret base door, vault chest, diamond ore) to mark it as a permanent beacon.
- **Sonar Ping**: Right-click in the air anytime to ping the radar — it gives live distance in meters, elevation (Above ⬆ / Below ⬇), and compass heading (North, South, East, West)!

### 3. 🕶️ Tactical Spy Goggles ([`SpyGogglesItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/SpyGogglesItem.java))
- Wear them in your Helmet armor slot!
- **Night Vision**: Instant, permanent clear vision in pitch-black caves and underwater.
- **Beacon Light Pillar**: Projects a vertical pillar of glowing light straight up from your tracked Block Beacon through terrain and walls!
- **Target Wall-Hack**: Tracked mobs continuously glow through solid walls.
- **Live Action-Bar HUD**: Displays a real-time tactical readout directly above your hotbar (`[SPY HUD | 📍 Base: 24m | 📡 Target: 12m | 💣 Armed C4: 2]`).

### 4. 💣 Tactical C4 Explosive & Remote Detonator ([`C4Block.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/block/C4Block.java) & [`RemoteDetonatorItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/RemoteDetonatorItem.java))
- Place 1 or more C4 explosive blocks on the ground, walls, or enemy doors.
- **Arm / Sync**: Right-click the placed C4 blocks with the Remote Detonator (the detonator beeps and links to the charges).
- **Detonate**: Walk away to a safe distance (or grapple up to a roof!), and right-click the Detonator in the air:
  - *CLICK! BEEP-BEEP-BEEP...* **BOOOOOM!**
  - All linked C4 blocks explode simultaneously in a coordinated breach!

### 5. ⚡ Classic Weapons
- **Erik's Star** ([`EriksStarItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/EriksStarItem.java)): Right-click to summon lightning where you look.
- **Erik's Lightning Sword** ([`ModItems.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/ModItems.java)): Netherite-grade blade (+7 attack, fast swing speed).

---

## 🧭 Project Layout

```
~/Develop/mc/
├── README.md                      # This guide
├── docs/                          # In-depth beginner guides
│   ├── 01-architecture-and-concepts.md
│   ├── 02-how-to-code-new-items-blocks-powers.md
│   ├── 03-making-textures-and-models.md
│   └── 04-playing-multiplayer-with-erik.md
└── erikcraft/                     # The Fabric mod project
    ├── build.gradle               # Build configuration (Fabric Loom 1.17 + Java 21)
    ├── gradle.properties          # Minecraft 1.21.1 & Java 21 toolchain
    ├── .vscode/                   # Cursor / VS Code launch & debug settings (F5 ready!)
    ├── src/main/java/com/erikcraft/
    │   ├── ErikCraftMod.java      # Main mod entrypoint
    │   ├── block/
    │   │   ├── C4Block.java       # Remote explosive block logic
    │   │   └── ModBlocks.java     # Block registry
    │   └── item/
    │       ├── GrapplingHookGunItem.java # Pull physics & cable particles
    │       ├── MobTrackerItem.java       # Mob & block beacon tracking
    │       ├── SpyGogglesItem.java       # Night vision, HUD & beacon beam
    │       ├── RemoteDetonatorItem.java  # Multi-charge C4 remote detonation
    │       ├── EriksStarItem.java        # Lightning strike
    │       ├── ModItems.java             # Items registry
    │       └── ModItemGroups.java        # Custom creative tab
    └── src/main/resources/assets/erikcraft/
        ├── lang/en_us.json        # Display names
        ├── models/                # 3D/2D models
        └── textures/              # 16x16 PNG pixel art
```

---

## 🛠️ Common Commands

Run these inside `~/Develop/mc/erikcraft`:

| Command | What it Does |
| :--- | :--- |
| `./gradlew runClient` | Boots up Minecraft with ErikCraft for live testing |
| `./gradlew build` | Compiles into `build/libs/erikcraft-1.0.0.jar` |
| `./gradlew runServer` | Runs a local dedicated test server |

---

## 🤝 Playing Together with Erik on Home Wi-Fi

1. Run `./gradlew build` in `~/Develop/mc/erikcraft`.
2. Find `build/libs/erikcraft-1.0.0.jar`.
3. Open **Prism Launcher** (installed in `/Applications/Prism Launcher.app`).
4. Create a **1.21.1 Fabric** instance and drop `erikcraft-1.0.0.jar` + `Fabric API` into `mods`.
5. In game: Press `Esc` -> **Open to LAN**. Erik joins from the **Multiplayer** menu!
