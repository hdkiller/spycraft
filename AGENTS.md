# Agent Guidelines (AGENTS.md)

This file is the primary context for AI agents (Antigravity, Gemini CLI, Claude Code, Cursor) working in this repository.

## Operator

**László Rácz — "hdkiller", "HDKiller", "László", "Laszlo", "@hdkiller", `hdkiller@gmail.com` — is the owner of this repository, the solo founder of Watt Mind, and Erik's Dad.** László and Erik are the co-creators of SpyCraft. The operator is the sole human in the loop: all decisions, reviews, and git merges report directly to him.

## Repository Purpose

This is the Minecraft Modding workspace (`~/Develop/mc`), housing **SpyCraft** (`com.hdkiller.spycraft`), a custom tactical spy and infiltration gear mod built on **Minecraft 1.21.1** with **Fabric Loader**.

- **GitHub Repository:** [`hdkiller/spycraft`](https://github.com/hdkiller/spycraft)
- **Mod ID:** `spycraft`
- **Root Java Package:** `com.hdkiller.spycraft`

## Project Management & Tracking

- **Linear Team:** Tracked in Linear under team **`LAB`** (e.g. issues `LAB-576`, `LAB-577`).
- **Commit Convention:** Every commit MUST reference its Linear issue: `type(scope): message (LAB-xxx)` (e.g. `feat(drone): add night vision HUD (LAB-123)`).
- **Personal Delivery Exception:** As a personal repository, agents may commit and push directly to `master` after self-review, clean build verification (`./gradlew build`), and testing. No pull request or branch waiting is required unless explicitly requested.

## Runtime & Toolchain

- **Java Version:** **Java 21 (LTS)**
  - Homebrew OpenJDK 21 symlink: `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`
  - Java executable: `/opt/homebrew/opt/openjdk@21/bin/java`
- **Minecraft:** `1.21.1`
- **Fabric Loader:** `0.19.5`
- **Fabric Loom:** `1.17.21`
- **Fabric API:** `0.116.17+1.21.1`
- **Gradle:** Wrapper located at `spycraft/gradlew`

## Workspace Layout

```
~/Develop/mc/
├── .vscode/                 # Preconfigured VS Code / Antigravity IDE settings, tasks & launch configs
│   ├── settings.json        # Java 21 runtime and Gradle wrapper configuration
│   ├── tasks.json           # Run Client, Build JAR, Run Server tasks
│   ├── launch.json          # F5 Debug launcher for Minecraft Client & Server
│   └── extensions.json     # Recommended Java & Gradle extensions
├── mc.code-workspace        # Multi-root VS Code workspace file
├── docs/                    # Architecture, Modding Guides, and Documentation
│   ├── 01-architecture-and-concepts.md
│   ├── 02-how-to-code-new-items-blocks-powers.md
│   ├── 03-making-textures-and-models.md
│   ├── 04-playing-multiplayer-with-erik.md
│   └── 05-ide-and-development-setup.md
├── spycraft/                # Main Fabric Mod project
│   ├── build.gradle         # Loom build configuration
│   ├── gradle.properties    # Mod metadata, Minecraft & dependency versions
│   ├── settings.gradle      # Root project name ('spycraft')
│   ├── gradlew              # Gradle wrapper script
│   └── src/
│       ├── main/java/com/hdkiller/spycraft/   # Server & shared game logic
│       │   ├── block/       # Custom blocks (SoundTrap, C4, LaserPylon)
│       │   ├── drone/       # Recon drone manager, flight logic, battery
│       │   ├── effect/      # Mob effects (DronePiloting)
│       │   ├── item/        # Custom items (SniperRifle, Tracker, Goggles, Disguise)
│       │   ├── laser/       # Laser forcefield perimeter manager
│       │   ├── mixin/       # Shared mixins (ServerPlayerGameMode, Player)
│       │   ├── sound/       # Sound trap manager
│       │   └── SpyCraftMod.java # Mod initializer entrypoint
│       ├── client/java/com/hdkiller/spycraft/client/ # Client-only rendering
│       │   ├── mixin/       # Client mixins (GuiMixin, PlayerRendererMixin, ItemInHandRendererMixin)
│       │   └── SpyCraftModClient.java # Client mod initializer
│       └── main/resources/  # Assets, textures, models, lang, fabric.mod.json
│           ├── assets/spycraft/
│           │   ├── blockstates/
│           │   ├── lang/en_us.json
│           │   ├── models/ (item/ & block/)
│           │   └── textures/ (item/ & block/)
│           ├── fabric.mod.json
│           └── spycraft.mixins.json
├── README.md                # Player-facing guide to SpyCraft gear and quickstart
└── AGENTS.md                # This file (Agent operational protocol)
```

## Common Commands

Run all build and launch commands from the `spycraft/` directory:

| Action | Command | Description |
| :--- | :--- | :--- |
| **Launch Client** | `cd spycraft && ./gradlew runClient` | Boots Minecraft 1.21.1 with SpyCraft loaded |
| **Build Mod JAR** | `cd spycraft && ./gradlew build` | Produces `spycraft/build/libs/spycraft-1.0.0.jar` |
| **Run Dedicated Server** | `cd spycraft && ./gradlew runServer` | Boots standalone Fabric test server |
| **Regenerate IDE Runs** | `cd spycraft && ./gradlew vscode` | Updates Loom launch cache and configs |

## Architectural Rules & Standards

### 1. Registration
- All items must be registered in [`ModItems.java`](spycraft/src/main/java/com/hdkiller/spycraft/item/ModItems.java) using `ResourceLocation.fromNamespaceAndPath(SpyCraftMod.MOD_ID, name)`.
- All blocks must be registered in [`ModBlocks.java`](spycraft/src/main/java/com/hdkiller/spycraft/block/ModBlocks.java).
- All creative tabs in [`ModItemGroups.java`](spycraft/src/main/java/com/hdkiller/spycraft/item/ModItemGroups.java).
- All effects in [`ModEffects.java`](spycraft/src/main/java/com/hdkiller/spycraft/effect/ModEffects.java).

### 2. Client vs. Server Separation
- **Client code** (GUI, HUD rendering, 3D model mixins, `Minecraft.getInstance()`, `LocalPlayer`) MUST live in `src/client/java` and be registered in `spycraft.client.mixins.json`.
- **Server code** (`ServerPlayer`, `ServerLevel`, game logic, damage sources) MUST never import client classes (`net.minecraft.client.*`). Violating this causes dedicated server crashes!

### 3. Memory Safety (Anti-Leak Rule)
- **NEVER** store `LivingEntity` or `Level` / `World` references in static maps or collections.
- Always store entity `UUID`s and lookup live entities via `level.getEntity(uuid)` or `server.getPlayerList().getPlayer(uuid)`.
- Disconnected players and dead mobs must be pruned on every tick or listener event (`ServerPlayConnectionEvents.DISCONNECT`).

### 4. Multi-Dimension Isolation
- Always pair coordinates with `ResourceKey<Level> dimension` (e.g. `record ChargeLocation(ResourceKey<Level> dimension, BlockPos pos)`).
- When ticking or rendering radar pings, verify that `dimension.equals(level.dimension())`. Never compute distance or spawn particles in the Overworld using Nether/End coordinates.

### 5. Persistent Ticking Blocks
- For blocks that must tick continuously across world reloads and server restarts (like the Sonic Sound Trap), use Minecraft's native `level.scheduleTick(pos, this, delay)`. Scheduled ticks are saved to chunk NBT automatically.

## VS Code & Antigravity IDE Integration

- Open `~/Develop/mc` in VS Code or Antigravity IDE.
- Press **F5** to start debugging Minecraft Client immediately with breakpoints enabled.
- Open the Command Palette (`Cmd+Shift+P`) and choose **Tasks: Run Task** $\rightarrow$ **Run Minecraft Client** or **Build SpyCraft Mod**.
