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

## 📦 What's Inside `ErikCraft` Right Now

We built a complete, working starter mod named **`erikcraft`** with real custom items ready to try:

1. **⚡ Erik's Star** ([`EriksStarItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/EriksStarItem.java)):
   - A glowing golden artifact.
   - **Right-click anywhere** to summon a thunderous lightning bolt right where you're aiming, complete with sound effects and a chat announcement!
2. **🗡️ Erik's Lightning Sword** ([`ModItems.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/ModItems.java)):
   - A custom Netherite-grade sword with boosted attack damage (+7) and fast attack speed.
3. **🎨 Custom Creative Tab** ([`ModItemGroups.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/ModItemGroups.java)):
   - Open Creative Inventory in-game — you'll see a dedicated **"ErikCraft Items"** tab displaying Erik's Star and Sword!
4. **🎨 Pixel Art Textures**:
   - 16x16 hand-crafted textures for both items in [`src/main/resources/assets/erikcraft/textures/item/`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/resources/assets/erikcraft/textures/item/).

---

## 🧭 How Everything Fits Together

```
~/Develop/mc/
├── README.md                      # This guide
├── docs/                          # In-depth beginner guides
│   ├── 01-architecture-and-concepts.md
│   ├── 02-how-to-code-new-items-blocks-powers.md
│   ├── 03-making-textures-and-models.md
│   └── 04-playing-multiplayer-with-erik.md
└── erikcraft/                     # The Fabric mod project
    ├── build.gradle               # Build & Loom configuration
    ├── gradle.properties          # Java 21 & Fabric versions
    ├── .vscode/                   # Cursor / VS Code launch & debug settings
    ├── src/main/java/com/erikcraft/
    │   ├── ErikCraftMod.java      # Main mod entrypoint
    │   └── item/
    │       ├── ModItems.java      # Item registry
    │       ├── ModItemGroups.java  # Custom creative tab
    │       └── EriksStarItem.java # Lightning ability logic
    └── src/main/resources/
        ├── fabric.mod.json        # Mod metadata (ID, version, name)
        └── assets/erikcraft/
            ├── lang/en_us.json    # Item display names
            ├── models/item/       # 3D/2D item models
            └── textures/item/     # 16x16 PNG pixel art
```

---

## 🛠️ Common Commands Cheatsheet

Run these inside `~/Develop/mc/erikcraft`:

| Command | What it Does |
| :--- | :--- |
| `./gradlew runClient` | Boots up Minecraft with your mod for testing |
| `./gradlew build` | Compiles your mod into `build/libs/erikcraft-1.0.0.jar` |
| `./gradlew runServer` | Runs a dedicated local test server |
| `./gradlew --status` | Checks running Gradle daemons |

---

## 🤝 Playing Together with Erik on Home Wi-Fi

1. Run `./gradlew build` in `~/Develop/mc/erikcraft`.
2. Find the output file: `build/libs/erikcraft-1.0.0.jar`.
3. Open **Prism Launcher** (already installed in `/Applications/Prism Launcher.app`).
4. Create a **1.21.1 Fabric** instance and drop `erikcraft-1.0.0.jar` + `Fabric API` into the `mods` folder.
5. In game: Press `Esc` -> **Open to LAN**. The other player can join immediately from the **Multiplayer** menu over your home Wi-Fi!

*(See [docs/04-playing-multiplayer-with-erik.md](file:///Users/hdkiller/Develop/mc/docs/04-playing-multiplayer-with-erik.md) for full screenshots and walkthrough).*

---

## 🎨 Recommended Next Tool for Erik: Blockbench

To draw custom textures or create 3D blocks and custom creatures together:
```bash
brew install --cask blockbench
```
Blockbench is free, built specifically for Minecraft, and lets you paint pixel art directly onto 3D models!
