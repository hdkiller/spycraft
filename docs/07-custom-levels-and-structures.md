# Creating Custom Levels, Structures & Populating Worlds

This guide explains how to bring large custom builds (such as skyscrapers created in Minebench, WorldEdit, or external 3D voxel tools) into Minecraft 1.21.1, integrate them into the **SpyCraft** mod, and populate them with custom mobs, traps, and mission objectives.

---

## 1. The Three Approaches to Custom "Levels"

In Minecraft, a "level" or "mission" can be implemented in one of three ways depending on your vision:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        HOW DO PLAYERS ENTER IT?                        │
├─────────────────────────┬─────────────────────────┬────────────────────┤
│ 1. Curated Mission Map  │ 2. World-Gen Structure  │ 3. In-Game Spawner │
│ (Adventure World)       │ (Procedural Discovery)  │ (Command / Gadget) │
├─────────────────────────┼─────────────────────────┼────────────────────┤
│ A pre-saved world folder│ Spawns naturally during │ Player runs a      │
│ with predefined spawn,  │ terrain generation in   │ command or uses an │
│ boundaries, and quests. │ biomes (like a Mansion).│ item to build it.  │
└─────────────────────────┴─────────────────────────┴────────────────────┘
```

### Approach 1: Curated Mission Map (Best for Erik & Adventure Missions)
* You build a dedicated world (e.g. `Spycraft_HQ_Level_1`).
* The skyscraper sits at specific coordinates with a fixed spawn point, adventure game mode (`/gamemode adventure`), lighting, and locked doors.
* You distribute the world folder alongside the mod, so you and Erik load directly into the mission!

### Approach 2: World Generation Structure (Best for Exploration)
* The skyscraper is saved as a Minecraft Structure Template (`.nbt`).
* Registered in the mod's DataPack under `data/spycraft/worldgen/structure/`.
* When wandering through a Plains or Desert biome, players spot the giant skyscraper on the horizon!

### Approach 3: In-Game Runtime Placement (`/spycraft spawn_skyscraper`)
* The mod provides an administrative or mission command (or an item like a "Deployable Satellite Base").
* Reads the structure file directly and pastes it right where the player is looking, then immediately spawns the guards.

---

## 2. Exporting and Converting from Minebench

External voxel/AI tools like Minebench generally output:
1. **`.schem` / `.schematic`** (WorldEdit / Sponge format)
2. **`.nbt`** (Vanilla Minecraft Structure Block format)
3. **`.obj` / `.vox`** (Generic 3D voxel models)

### How to get it into Minecraft Java 1.21.1:

#### If Minebench gives you `.schem` or `.schematic`:
1. Use **WorldEdit** or **Axiom** mod in a singleplayer creative world.
2. Put the `.schem` file into `.minecraft/config/worldedit/schematics/`.
3. In game, run:
   ```mcfunction
   //schem load skyscraper
   //paste
   ```
4. Save it as an official vanilla `.nbt` using Minecraft's built-in **Structure Block**:
   - Place a Structure Block in **Save mode**.
   - Note: Vanilla structure blocks have a maximum size of **48 × 48 × 48** blocks per box. For tall skyscrapers (e.g. 120 blocks tall), you save it in vertical slices: `skyscraper_base.nbt`, `skyscraper_mid.nbt`, `skyscraper_roof.nbt` (or use the mod's multi-piece loader).

---

## 3. Saving Structures Inside the SpyCraft Mod

Vanilla Minecraft structures live inside the mod jar under the assets/data path:

```
spycraft/src/main/resources/data/spycraft/structure/
├── skyscraper_base.nbt
├── skyscraper_floor.nbt
└── skyscraper_penthouse.nbt
```

When packaged inside the mod, any client or server running SpyCraft can access the structure without needing external file copies!

---

## 4. Populating the Skyscraper with Mobs

Once the building is placed, how do we fill it with enemies, guards, and security systems?

### Method A: Data-Driven Spawner Blocks in the Structure
While building the skyscraper in Creative mode:
1. Place **Monster Spawner** blocks (`minecraft:spawner`) hidden inside ventilation shafts, behind desks, or in elevators.
2. Configure the spawners to spawn custom mobs (e.g. Pillagers with bows, Vindicators with iron axes, or custom SpyCraft guards).
3. When the structure is saved to `.nbt`, the spawner data is saved with it! Whenever the structure spawns, the spawners are already configured.

### Method B: Java Code Programmatic Population (Recommended for Bosses & Missions)
In SpyCraft, we can write a dedicated `MissionStructureManager` in Java that places the building and populates each floor:

```java
public class SkyscraperMission {
    public static void spawnMission(ServerLevel level, BlockPos basePos) {
        // 1. Paste the building from NBT
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> template = manager.get(ResourceLocation.fromNamespaceAndPath("spycraft", "skyscraper"));
        
        template.ifPresent(struct -> {
            struct.placeInWorld(level, basePos, basePos, new StructurePlaceSettings(), level.random, Block.UPDATE_ALL);
            
            // 2. Populate Floor 1: Security Guards
            spawnGuard(level, basePos.offset(5, 1, 5), "Lobby Guard");
            spawnGuard(level, basePos.offset(15, 1, 5), "Lobby Guard");
            
            // 3. Populate Floor 2: Laser Traps (using our mod's blocks!)
            level.setBlock(basePos.offset(2, 6, 2), ModBlocks.LASER_PYLON.defaultBlockState(), 3);
            level.setBlock(basePos.offset(10, 6, 2), ModBlocks.LASER_PYLON.defaultBlockState(), 3);
            level.setBlock(basePos.offset(10, 6, 10), ModBlocks.LASER_PYLON.defaultBlockState(), 3);
            
            // 4. Populate Penthouse: The Enemy Mastermind (Boss)
            spawnBoss(level, basePos.offset(10, 40, 10));
        });
    }

    private static void spawnGuard(ServerLevel level, BlockPos pos, String name) {
        Pillager guard = EntityType.PILLAGER.create(level);
        if (guard != null) {
            guard.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            guard.setCustomName(Component.literal("§c" + name));
            guard.setCustomNameVisible(true);
            // Give guard custom gear or SpyCraft items
            guard.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.NIGHT_VISION_GOGGLES));
            level.addFreshEntity(guard);
        }
    }
}
```

---

## 5. Adding SpyCraft Mechanics to the Level

To make the skyscraper feel like a real spy mission:
1. **Laser Grids:** Place `LaserPylonBlock` rows across hallways that trigger alarms or block access to the elevators.
2. **C4 Vault Doors:** Reinforce doors with Obsidian or Iron, requiring Erik to place and detonate a `C4Block` to breach.
3. **Sound Decoys:** Guard mobs can have AI that investigates sounds when a `SoundTrapBlock` is activated.
4. **Security Cameras / Recon Drones:** Players must use the `ReconDroneItem` to scout upper floors before charging in!

---

## 7. The Implemented System: Mission Beacon & Procedural Base

In SpyCraft, the mission deployment system is implemented in [`SpyBaseMissionBuilder.java`](file:///Users/hdkiller/Develop/mc/spycraft/src/main/java/com/hdkiller/spycraft/world/SpyBaseMissionBuilder.java) and triggered via [`MissionBeaconItem.java`](file:///Users/hdkiller/Develop/mc/spycraft/src/main/java/com/hdkiller/spycraft/item/MissionBeaconItem.java):

### Key Implementation Mechanics:
1. **Single-Use Item & World Lock:**
   - When right-clicked on the ground, the item shrinks by 1 (`context.getItemInHand().shrink(1)`).
   - A persistent lock file (`spycraft_mission_active.lock`) is written to the world folder. If players attempt to deploy a second beacon in the same world, deployment is rejected to preserve performance.
   - Server operators can reset this with `/spycraft reset_mission`.
2. **Subterranean Level Offset:**
   - Skyscraper structures that feature an underground parking garage or secret bunker are positioned with an automatic Y-offset so that the garage is buried beneath the surface ground level and the lobby sits flush on the terrain.
3. **Biome & Vegetation Clearing:**
   - Clears surrounding trees, leaves, and obstructions in a protective clearance bounding box around the structure footprint to prevent biome terrain from clipping into the skyscraper interior.
4. **Player Safety Relocation:**
   - Automatically teleports the activating player to a safe vantage point directly outside the entrance on the surface, preventing players from being trapped beneath the structure foundation during generation.
