# Publishing, Releases & Steam Deck Deployment

This guide documents the complete distribution, automated deployment, and release lifecycle of the **SpyCraft** mod.

---

## 1. Steam Deck Deployment & Handheld Optimization

SpyCraft is optimized for playing handheld on the Steam Deck using **Prism Launcher** (Flatpak).

### Automated 1-Command Deployment

The repository includes a dedicated sync script:

```bash
# Build and transfer to Steam Deck over SSH
./scripts/sync-steamdeck.sh

# Fast sync (skips Gradle build if jar is already compiled)
./scripts/sync-steamdeck.sh --skip-build
```

#### How it works:
1. Verifies SSH connectivity to `steamdeck` (`192.168.1.156`, user `deck`).
2. Automatically locates Prism Launcher instances (`~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/1.21.1/minecraft/mods`).
3. Safely transfers the compiled `spycraft-1.0.0.jar`.
4. Sends an on-device desktop notification via `notify-send` so Erik or the player knows the update landed!

### Recommended Steam Deck Mod Stack (Fabric 1.21.1)

To get smooth 60 FPS and responsive controller support on Steam Deck:

| Mod | Purpose |
| :--- | :--- |
| **Sodium** | Massive rendering engine rewrite, boosts framerates 2-3x |
| **Lithium** | General physics and game logic optimization |
| **FerriteCore** | Memory usage reduction |
| **ModernFix** | Faster launch times and memory leak protection |
| **Controlify** | Native Steam Deck controller support with gyro & haptics |
| **Iris Shaders** | Modern, fast shader engine compatible with Sodium |

### Shaders on Steam Deck

Shaders are installed in the instance directory:
`~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/1.21.1/minecraft/shaderpacks/`

- **Complementary Reimagined (`ComplementaryReimagined_r5.9.3.zip`):** High-end cinematic lighting and water.
- **MakeUp Ultra Fast (`MakeUp-UltraFast-9.5g.zip`):** Lightweight shader for high battery life and steady 60 FPS.

---

## 2. Modrinth Distribution

SpyCraft is hosted on **Modrinth**, the primary open-source platform for modern Minecraft mods.

- **Project Page:** [https://modrinth.com/mod/spycraft-mod](https://modrinth.com/mod/spycraft-mod)
- **Project ID:** `JwalTGKJ`
- **Slug:** `spycraft-mod`
- **Categories:** Technology, Adventure, Equipment
- **Environment:** Client & Server (Universal)
- **Dependency:** [Fabric API](https://modrinth.com/mod/fabric-api) (`P7dR8mSH`)

### Automated Publishing via Modrinth API v2

Releases can be published programmatically using personal access tokens:

- **Endpoint:** `POST https://api.modrinth.com/v2/version`
- **Authentication:** `Authorization: <MODRINTH_TOKEN>` in header
- **User-Agent:** `hdkiller/spycraft/1.0.0 (hdkiller@gmail.com)`
- **Secrets Management:** The Modrinth token is stored in the gitignored `.env` file (`MODRINTH_TOKEN=...`). Never commit secrets to git.

---

## 3. GitHub Releases & CI/CD Pipeline

The public repository lives at **[github.com/hdkiller/spycraft](https://github.com/hdkiller/spycraft)**.

### Automated Release Workflow (`.github/workflows/release.yml`)

Whenever a version tag is pushed:

```bash
git tag v1.0.1
git push origin v1.0.1
```

The GitHub Actions runner:
1. Checks out the repository.
2. Sets up Temurin JDK 21.
3. Runs the automated JUnit 5 test suite (`./gradlew test`).
4. Builds the mod jar (`./gradlew build`).
5. Creates a GitHub Release and attaches `spycraft-1.0.1.jar` with automated release notes.

---

## 4. Dual-Language Localization Architecture

SpyCraft supports native internationalization through Minecraft's built-in translation engine:

### Translation Files
- **`spycraft/src/main/resources/assets/spycraft/lang/en_us.json`:** Clean English default for international players.
- **`spycraft/src/main/resources/assets/spycraft/lang/hu_hu.json`:** Native Hungarian translation for all items, tooltips, and system alerts.

### Implementation Standard: `Component.translatable(...)`
Never hardcode raw text in Java items or tooltips. Always use translation keys:

```java
// Item tooltips
tooltip.add(Component.translatable("tooltip.spycraft.parachute_backpack.desc"));

// In-game messages with formatting arguments
player.sendSystemMessage(Component.translatable("tooltip.spycraft.recon_drone.battery", pct, secs));
```

### Automated Translation Verification Tests
The JUnit test class [`SpyGadgetsTest.java`](file:///Users/hdkiller/Develop/mc/spycraft/src/test/java/com/hdkiller/spycraft/SpyGadgetsTest.java) verifies that every gadget exists in both `en_us.json` and `hu_hu.json`, preventing accidental regressions before building releases.

---

## 5. In-Game Admin & Mission Commands

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/spycraft reset_mission` | Operator (level 2) | Clears the `spycraft_mission_active.lock` in the world folder, allowing a new villain skyscraper mission base to be deployed |
