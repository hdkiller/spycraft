<p align="center">
  <img src="docs/spycraft_logo_512.png" width="180" alt="SpyCraft Logo" />
</p>

# SpyCraft: Tactical Secret Agent Gear for Minecraft

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://fabricmc.net/)
[![Fabric Loader](https://img.shields.io/badge/Fabric-0.16%2B-blue.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Modrinth](https://img.shields.io/badge/Modrinth-spycraft--mod-00AF5C.svg)](https://modrinth.com/mod/spycraft-mod)
[![GitHub Release](https://img.shields.io/github/v/release/hdkiller/spycraft?color=success&label=Release)](https://github.com/hdkiller/spycraft/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**SpyCraft** is a tactical secret agent mod for Minecraft 1.21.1 (Fabric). It introduces controllable reconnaissance drones, multi-pylon laser forcefield perimeters, night and thermal vision, emergency parachute backpacks, tranquilizer darts, and procedural villain skyscraper infiltration missions.

Built by **László & Erik**. Supports native **English (default)** and **Hungarian (Magyar)** in-game localization.

---

## Requirements & Installation

### Requirements
- **Minecraft:** `1.21.1`
- **Mod Loader:** `Fabric Loader >= 0.16.0`
- **Java:** `21`
- **Required Dependency:** [Fabric API](https://modrinth.com/mod/fabric-api)

### Installation
1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.1.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and place it into your `.minecraft/mods` directory.
3. Download the latest `spycraft-1.0.0.jar` from [Modrinth](https://modrinth.com/mod/spycraft-mod) or [GitHub Releases](https://github.com/hdkiller/spycraft/releases) and place it into your `.minecraft/mods` directory.
4. Launch Minecraft using your preferred launcher (Prism Launcher, Modrinth App, or official Minecraft launcher).

---

## Tactical Arsenal Overview

All items and blocks are available in creative mode under the **"SpyCraft: Spy & Adventure Gear"** tab.

### 1. Tactical Sniper Rifle
- **Optical Scope Zoom:** Hold right-click to zoom in on distant targets with high magnification.
- **Ballistics:** Release right-click to fire a hypersonic shot with visual smoke and spark trails up to 128 blocks away.
- **Damage:** Delivers high precision damage (28 HP), capable of neutralizing most hostiles in a single hit.
- **Hip Fire:** Sneak + right-click to fire instantly without scoping.
- **Thermal Pairing:** When paired with Tactical Spy Goggles, targets highlight through solid walls for clear target acquisition.

### 2. Laser Forcefield Security Grid
- **Laser Security Pylons:** Deploy three or more pylons around an area to define a secure perimeter.
- **Laser Remote:** Click pylons in sequence, then click into the air to engage the system.
  - **Active State:** Generates an impenetrable 4-block high laser energy barrier between connected pylons.
  - **Shock Repulsion:** Intruders attempting to breach the perimeter receive electrical shock damage and are knocked back.
  - **Deactivation:** Right-click into the air again to instantly toggle off the grid.

### 3. Tactical Recon Drone
- **Controllable Flight:** Right-click to pilot the drone with full 3D directional flight, animated propellers, and navigation LEDs (60-second flight battery).
- **Custom Cockpit HUD Dock:** Player hotbar is replaced with a dedicated 4-action tactical drone dock:
  1. `[1: Laser Target Marker]` &mdash; Designates targets with a red laser beacon visible up to cloud level.
  2. `[2: Tranquilizer Dart (5/5)]` &mdash; Fires pneumatic sleep darts, immobilizing targets for 15 seconds.
  3. `[3: Kamikaze Strike]` &mdash; Initiates a tactical dive bomb explosion (yield 4.0), safely returning the pilot to base.
  4. `[4: Return to Base]` &mdash; Recalls the drone and safely returns control to the player.
- **Recharging:** Sneak + right-click with Redstone Dust to instantly recharge battery and refill darts. Slowly trickles charge while resting in inventory.
- **Radar Scanner:** Passively scans nearby mobs within 32 meters, highlighting them with glowing outlines synchronized to spy goggles.

### 4. Tactical Parachute Backpack
- **Worn Mode (Chestplate Slot):** Automatically deploys when falling from high structures or cliffs. Can also be manually deployed during descent by pressing the Sneak key.
- **Hand Mode (Inventory):** Right-click while in mid-air to deploy directly from hand as an emergency contingency.
- **Gliding Mechanics:** Smooth, steerable descent in the direction the player is looking, completely eliminates fall damage, and automatically repacks upon landing.

### 5. Magnetic Climbing Gloves
- **Vertical Surface Scaling:** Hold in hand to climb vertical walls, including glass, smooth stone, and metal structures.
- **Wall Cling:** Press Sneak (Shift) to latch onto walls and hold position for sniping or recon.

### 6. Thermal Vision Goggles
- **Wall Penetration:** Grants high-contrast target outlines through solid blocks within a 32-meter radius.
- **Night Operations:** Integrated night vision eliminates cave and darkness visibility penalties.

### 7. Tranquilizer Dart Gun
- **Pneumatic Stealth:** Fires silent darts that put hostile and neutral mobs to sleep for 15 seconds.
- **Aggro Reset:** Sleeping targets completely lose player aggression when waking up.

### 8. Holographic Decoy Projector
- **Target Diversion:** Right-click on the ground to project a lifelike holographic operative clone.
- **Threat Draw:** Nearby guards, monsters, and hostile mobs prioritize attacking the decoy for 25 seconds.

### 9. Tactical Smoke Grenade
- **Screening:** Thrown grenade creates a dense 12-second smoke cloud upon impact.
- **Tactical Concealment:** Blinds hostile mobs, breaks enemy line of sight, and grants the player stealth camouflage.

### 10. Tactical C4 Explosive & Remote Detonator
- **Variable Yield:** Supports 1x Standard (4.5x), 2x Double (9.0x), and 3x Mega Breach (18.0x) explosive charges.
- **Multi-Detonation:** Place multiple charges and detonate them simultaneously with the remote trigger from a safe distance.

### 11. Procedural Mission Skyscraper Base
- **Mission Deployer Beacon:** Right-click on the ground to construct a 96-floor villain corporate skyscraper.
- **Structure Features:** Multi-level subterranean parking garage, security checkpoints, drone sentries, laser barriers, executive penthouses, and mission safes.

### 12. Villager Spy Disguise
- **Infiltration Outfit:** Mask and Robe combination completely alters the player's 3D model into an authentic villager with custom animations.
- **Covert Sounds:** Right-clicking while disguised produces authentic villager vocalizations and emerald particle effects.

### 13. Tactical Grappling Hook Gun
- **Rapid Ascent:** Fires a reinforced cable up to 32 blocks to pull the operative to rooftops or elevated ledges.
- **Soft Landing:** Automatically provides brief slow-fall mitigation on arrival.

### 14. Spy Bug & Radar Tracker
- **Asset Tagging:** Attach to any player or mob to track position and distance for 10 minutes through solid terrain.
- **Waypoint Anchors:** Plant on blocks to mark permanent extraction points and secret bases.

### 15. Sonic Decoy Sound Trap
- **Acoustic Lure:** Emits realistic Creeper priming audio every 3 seconds with 2.5x volume range.
- **Mob Gathering:** Draws all hostile and neutral entities within 32 blocks directly toward the decoy.

### 16. Tactical Recon Binoculars
- **Optical Zoom:** Hold right-click to engage high-magnification long-range optical zoom (up to 96 blocks).
- **Dwell Lock-on Tagging:** Hovering the optical crosshairs over an enemy for 1.2 seconds locks on with rising audio pitch feedback.
- **Wall-Penetrating Intel:** Once locked, the target gains the Glowing effect (visible through walls for 5 minutes) and automatically synchronizes with the operative's Spy Radar and GPS visor.

---

## Development & Testing

The repository includes a complete Gradle build environment and automated JUnit 5 test suite.

```bash
# Run tests
./gradlew test

# Compile mod JAR
./gradlew build

# Launch client development environment
./gradlew runClient

# Launch dedicated multiplayer test server
./gradlew runServer
```

### Texture Variant Studio
SpyCraft features an asset harness for inspecting textures and 3D models:

```bash
# Start asset preview server at http://127.0.0.1:8088
python3 tools/asset-harness/server.py

# Apply tactical texture preset
python3 tools/asset-harness/cli_apply.py apply-preset specops
```

---

## Localization

SpyCraft is localized with clean, native language support:
- **English (`en_us`):** Default standard terminology.
- **Hungarian (`hu_hu`):** Complete native localization for all items, tooltips, and in-game status prompts.

---

## License

This project is licensed under the [MIT License](LICENSE).
