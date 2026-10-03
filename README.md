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

### 1. ⚡ Lézerfal Erőpajzs Csapda (Laser Forcefield Trap) ([`LaserPylonBlock.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/block/LaserPylonBlock.java) & [`LaserRemoteItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/LaserRemoteItem.java))
- **Lézeroszlopok (Pylons):** Helyezz le legalább 3 (vagy több) oszlopot a védeni kívánt terület körül (háromszög, téglalap, vagy tetszőleges sokszög).
- **Lézerfal Távirányító (Remote):**
  - **Összekötés:** Kattints jobb gombbal sorban az oszlopokra (a távirányító jelzi a zárt körletet).
  - **Aktiválás / Kikapcsolás:** Kattints a levegőbe a távirányítóval:
    - **BE:** Az oszlopok között 4 méter magas, ragyogó sci-fi lézer erőpajzs fal jelenik meg!
    - **ÁTHATOLHATATLAN:** Bármilyen mob vagy játékos (Erik, zombik, Apa) próbál átjutni rajta: **az erőpajzs elektromos kisüléssel azonnal visszalöki őket és sebzi a betolakodót!** Senki sem tud kijönni vagy bemenni!
    - **KI:** Még egy kattintás a levegőbe $\rightarrow$ az erőpajzs azonnal leáll, a terület újra szabadon átjárható!
    - **Törlés:** Guggolva (Shift) + jobb-klikk törli a kijelölt hálózatot.

### 2. 🥸 Falusi Álcaruha (Villager Spy Disguise) ([`VillagerDisguiseItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/VillagerDisguiseItem.java))
- **Falusi Álcamaszk** (Sisak) és **Falusi Álcaköpeny** (Mellvért).
- **Valódi 3D Modellcsere:** Ha felveszed, a játékos modellje azonnal kicserélődik egy **élethű, animált Falusira** (összekulcsolt kezekkel, nagy orral, sétáló animációval és fejmozgással)!
- **Többjátékosban is:** LAN-on játszva Erik és te is igazi falusinak látjátok az álcázott játékost!
- **Hang:** Álcázva jobb-klikkre a klasszikus *"HRRRMMM!"* falusi hangot adja zöld smaragd szikrákkal!

### 3. 🔫 Tactical Grappling Hook Gun ([`GrapplingHookGunItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/GrapplingHookGunItem.java))
- Aim at any ledge or rooftop up to 32 blocks away and right-click.
- Shoots a cable with spark trails and reels Erik up with soft-landing slow-fall protection!

### 4. 📡 Spy Bug & Radar Tracker ([`MobTrackerItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/MobTrackerItem.java))
- **Mount on Mobs/Players**: Right-click ANY mob or Dad to plant a live bug (glows through walls for 10 min).
- **Plant GPS Beacon on Blocks**: Right-click ANY block (base chest, vault door) to plant a permanent beacon.
- **Sonar Ping**: Right-click in the air anytime to ping distance, elevation, and compass heading.

### 5. 🕶️ Tactical Spy Goggles ([`SpyGogglesItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/SpyGogglesItem.java))
- Wear in the Helmet slot.
- **Night Vision**: Instant, permanent clear vision in dark caves and at night.
- **Sky-High Beacon Pillar**: Projects a towering **96-block tall glowing light pillar** straight up from your tracked Block Beacon into the sky, visible over mountains from up to 256 meters away!
- **Target Wall-Hack**: Tracked mobs continuously glow through solid walls.
- **Live Action-Bar HUD**: `[SPY HUD | 📍 Base: 24m | 📡 Target: 12m | 💣 C4: 2 [MEGA 3x]]`.

### 6. 🧭 GPS Waypoint Navigator Visor ([`GpsNavigatorGogglesItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/GpsNavigatorGogglesItem.java))
- Wear in the Helmet slot when you are far away and need turn-by-turn navigation!
- **Floating 3D Guidance Arrows**: Projects glowing 3D arrows directly in the air in front of your eyes pointing the exact way towards your beacon or target!
- **Dynamic Relative HUD**: Calculates which way to turn relative to where you are looking:
  - `⬆ [EGYENESEN ELŐRE]` (Straight ahead)
  - `↗ [ENYHÉN JOBBRA]`, `➡ [FORDULJ JOBBRA]`, `↘ [JOBBRA HÁTRA]`
  - `⬇ [FORDULJ MEG!]` (Turn around)
  - `↖ [ENYHÉN BALRA]`, `⬅ [FORDULJ BALRA]`, `↙ [BALRA HÁTRA]`

### 7. 🧨 Tactical C4 Canister & Remote Detonator ([`C4Block.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/block/C4Block.java) & [`RemoteDetonatorItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/RemoteDetonatorItem.java))
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
