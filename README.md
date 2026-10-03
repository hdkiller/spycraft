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

All tools are in the **"ErikCraft Spy & Adventure Gear"** creative tab:

### 1. 🔫 Tactical Grappling Hook Gun ([`GrapplingHookGunItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/GrapplingHookGunItem.java))
- Aim at any ledge or rooftop up to 32 blocks away and right-click.
- Shoots a cable with spark trails and reels Erik up with soft-landing slow-fall protection!

### 2. 📡 Spy Bug & Radar Tracker ([`MobTrackerItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/MobTrackerItem.java))
- **Mount on Mobs/Players**: Right-click ANY mob or Dad to plant a live bug (glows through walls for 10 min).
- **Plant GPS Beacon on Blocks**: Right-click ANY block (base chest, vault door) to plant a permanent beacon.
- **Sonar Ping**: Right-click in the air anytime to ping distance, elevation, and compass heading.

### 3. 🕶️ Tactical Spy Goggles ([`SpyGogglesItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/SpyGogglesItem.java))
- Wear in the Helmet slot.
- **Night Vision**: Instant, permanent clear vision in dark caves and at night.
- **Sky-High Beacon Pillar**: Projects a towering **96-block tall glowing light pillar** straight up from your tracked Block Beacon into the sky, visible over mountains from up to 256 meters away!
- **Target Wall-Hack**: Tracked mobs continuously glow through solid walls.
- **Live Action-Bar HUD**: `[SPY HUD | 📍 Base: 24m | 📡 Target: 12m | 💣 C4: 2 [MEGA 3x]]`.

### 4. 🧭 GPS Waypoint Navigator Visor ([`GpsNavigatorGogglesItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/GpsNavigatorGogglesItem.java))
- Wear in the Helmet slot when you are far away and need turn-by-turn navigation!
- **Floating 3D Guidance Arrows**: Projects glowing 3D arrows directly in the air in front of your eyes pointing the exact way towards your beacon or target!
- **Dynamic Relative HUD**: Calculates which way to turn relative to where you are looking:
  - `⬆ [EGYENESEN ELŐRE]` (Straight ahead)
  - `↗ [ENYHÉN JOBBRA]`, `➡ [FORDULJ JOBBRA]`, `↘ [JOBBRA HÁTRA]`
  - `⬇ [FORDULJ MEG!]` (Turn around)
  - `↖ [ENYHÉN BALRA]`, `⬅ [FORDULJ BALRA]`, `↙ [BALRA HÁTRA]`
  - Elevation indicators (`[FENT 🔼]`, `[LENT 🔽]`).

### 5. 🧨 Tactical C4 Canister & Remote Detonator ([`C4Block.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/block/C4Block.java) & [`RemoteDetonatorItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/RemoteDetonatorItem.java))
- Sleek 3D **cylindrical tube canister** with metal caps and 3 LED indicators.
- **3 Robbanási Fokozat (Yield Levels)**:
  - 1× jobb-klikk: **1. Fokozat (Alap 4.5x)** - tiszta falbontás.
  - 2× jobb-klikk: **2. Fokozat (Dupla 9.0x)** - dupla rombolóerő és kráter.
  - 3× jobb-klikk: **3. Fokozat (MEGA 18.0x)** - óriási mozi-szerű robbanás és sokkhullám!
- Right-click detonator in the air to trigger simultaneous breach!

---

## 🛠️ Common Commands

Run these inside `~/Develop/mc/erikcraft`:

| Command | What it Does |
| :--- | :--- |
| `./gradlew runClient` | Boots up Minecraft with ErikCraft for live testing |
| `./gradlew build` | Compiles into `build/libs/erikcraft-1.0.0.jar` |
