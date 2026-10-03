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

### 1. 🎯 Mesterlövész Puska (Tactical Sniper Rifle) ([`SniperRifleItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/SniperRifleItem.java))
- **Optikai Célkereszt Zoom:** Tartsd nyomva a jobb egérgombot $\rightarrow$ a kamera ráközelít a távoli célpontra nagy nagyítással!
- **Lövés:** Engedd el a gombot $\rightarrow$ dördül a lövés (hangos visszhang és visszarúgás), és egy hiperszonikus füst/szikracsík csapódik be akár **128 blokk** távolságba!
- **Extrém Sebzés:** 28 sebzés (egy lövésből teríti le a legtöbb szörnyet).
- **Csípőlövés:** Guggolva (Shift) + jobb gombbal azonnal lő célzás nélkül.
- **Kém Szemüveg Kombó:** Ha a **Tactical Spy Goggles** rajtad van, a célpontok a falakon át is ragyognak, így sötétben vagy fedezék mögül is láthatod őket!

### 2. ⚡ Lézerfal Erőpajzs Csapda (Laser Forcefield Trap) ([`LaserPylonBlock.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/block/LaserPylonBlock.java) & [`LaserRemoteItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/LaserRemoteItem.java))
- **Lézeroszlopok (Pylons):** Helyezz le legalább 3 oszlopot a védeni kívánt terület körül.
- **Távirányító (Remote):** Kattints sorban az oszlopokra, majd kattints a levegőbe:
  - **BE:** 4 méter magas, szikrázó sci-fi lézer erőpajzs fal jelenik meg az oszlopok között!
  - **ÁTHATOLHATATLAN:** Bárki megpróbál átjutni rajta, az erőpajzs elektromos kisüléssel visszalöki és megrázza! Senki sem tud kijönni vagy behatolni!
  - **KI:** Még egy kattintás a levegőbe $\rightarrow$ a lézerfal azonnal kikapcsol.

### 3. 🥸 Falusi Álcaruha (Villager Spy Disguise) ([`VillagerDisguiseItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/VillagerDisguiseItem.java))
- **Falusi Álcamaszk** (Sisak) és **Falusi Álcaköpeny** (Mellvért).
- **Valódi 3D Modellcsere:** Ha felveszed, a játékos modellje azonnal kicserélődik egy **élethű, animált Falusira** (összekulcsolt kezekkel, nagy orral, sétáló animációval és fejmozgással)!
- **Hang:** Álcázva jobb-klikkre a klasszikus *"HRRRMMM!"* falusi hangot adja zöld smaragd szikrákkal!

### 4. 🔫 Tactical Grappling Hook Gun ([`GrapplingHookGunItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/GrapplingHookGunItem.java))
- Aim at any ledge or rooftop up to 32 blocks away and right-click.
- Shoots a cable with spark trails and reels Erik up with soft-landing slow-fall protection!

### 5. 📡 Spy Bug & Radar Tracker ([`MobTrackerItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/MobTrackerItem.java))
- **Mount on Mobs/Players**: Right-click ANY mob or Dad to plant a live bug (glows through walls for 10 min).
- **Plant GPS Beacon on Blocks**: Right-click ANY block to plant a permanent beacon.
- **Sonar Ping**: Right-click in the air anytime to ping distance, elevation, and compass heading.

### 6. 🕶️ Tactical Spy Goggles ([`SpyGogglesItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/SpyGogglesItem.java))
- **Night Vision**: Instant, permanent clear vision in dark caves and at night.
- **Sky-High Beacon Pillar**: Projects a towering **96-block tall glowing light pillar** straight up into the sky, visible over mountains from up to 256 meters away!
- **Live Action-Bar HUD**: `[SPY HUD | 📍 Base: 24m | 📡 Target: 12m | 💣 C4: 2 [MEGA 3x]]`.

### 7. 🧭 GPS Waypoint Navigator Visor ([`GpsNavigatorGogglesItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/GpsNavigatorGogglesItem.java))
- **Floating 3D Guidance Arrows**: Projects glowing 3D arrows directly in the air in front of your eyes pointing the exact way towards your beacon or target!
- **Dynamic Relative HUD**: `⬆ [EGYENESEN ELŐRE]`, `➡ [FORDULJ JOBBRA]`, `⬇ [FORDULJ MEG!]`, stb.

### 8. 🧨 Tactical C4 Canister & Remote Detonator ([`C4Block.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/block/C4Block.java) & [`RemoteDetonatorItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/RemoteDetonatorItem.java))
- Sleek 3D **cylindrical tube canister** with 3 LED indicators.
- **3 Robbanási Fokozat**: 1x Alap (4.5x), 2x Dupla (9.0x), 3x MEGA (18.0x óriási kráter)!
- Right-click detonator in the air to trigger simultaneous breach!

### 9. 🛸 Felderítő Drón (Tactical Recon Drone) ([`ReconDroneItem.java`](file:///Users/hdkiller/Develop/mc/erikcraft/src/main/java/com/erikcraft/item/ReconDroneItem.java))
- **Irányítható repülés (15s)**: Jobb klikk és a játékos átveszi a drón irányítását szabad 3D repüléssel, propellerekkel és LED fényekkel!
- **Taktikai Drón HUD**: Teljes képernyős HUD akkumulátor töltöttségjelzővel, magasságmérővel, radarképpel és célkereszttel.
- **Radar & Szkenner**: Folyamatosan pásztázza a környező mobokat (32m hatótáv), Glowing körvonalat ad nekik és szinkronizál a Trackerekkel és Szemüvegekkel!
- **🎯 Lézeres Célmegjelölés (Laser Tag)**: Célpontra nézve jobb klikk lead egy vörös lézersugarat, 96 blokk magas beacon fénysugarat helyez a célpontra és rögzíti a koordinátáit!
- **💥 Kamikaze Önmegsemmisítés**: Shift + Jobb klikk hatására a drón lecsap és masszív taktikai robbanással megsemmisíti a célterületet, a pilóta pedig biztonságban visszakerül a bázisra.
- **🏠 Visszahívás**: Ég felé nézve jobb klikk (vagy az akku lejárta) azonnal és biztonságosan visszateportálja a pilótát a kiindulási pontra.

---

## 🛠️ Common Commands

Run these inside `~/Develop/mc/erikcraft`:

| Command | What it Does |
| :--- | :--- |
| `./gradlew runClient` | Boots up Minecraft with ErikCraft for live testing |
| `./gradlew build` | Compiles into `build/libs/erikcraft-1.0.0.jar` |
