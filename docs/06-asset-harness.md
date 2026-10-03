# SpyCraft Asset Studio & Texture Variant Harness

A specialized toolchain and interactive web studio for previewing, inspecting, comparing, and switching 16x16 pixel art textures for all SpyCraft items and blocks.

---

## 1. Quick Start

### A. Web Studio (Interactive UI)
Launch the local harness server:
```bash
python3 spycraft/tools/asset-harness/server.py
```
Then open your browser at:
**[http://127.0.0.1:8088](http://127.0.0.1:8088)**

#### Features in the Web Studio:
- **Side-by-Side Comparison:** View the original baseline sprite vs 3–4 handcrafted artistic variants for every item and block.
- **Pixel-Perfect Zoom:** Inspect sprites at 4x, 8x, or 12x magnification with nearest-neighbor pixelated rendering.
- **Background Switcher:** Toggle between Transparent Grid, Dark Slate, Minecraft Stone, Dirt, and Oak Planks to evaluate contrast in various in-game lighting conditions.
- **Interactive 3D Block Orbit View:** Inspect multi-face blocks (`C4 Explosive`, `Sound Trap`, `Laser Pylon`) mapped onto a 3D cube with interactive mouse rotation.
- **Hotbar & Tooltip Simulator:** Test in-hand appearance inside an authentic 9-slot Minecraft hotbar with custom tooltips.
- **1-Click Apply to Mod:** Click **Apply to Mod** on any card to immediately overwrite the active texture in `spycraft/src/main/resources/assets/spycraft/textures/`.
- **Theme Presets:** Switch the entire mod's art style in 1 click using the theme dropdown in the header.

---

### B. Command-Line Switcher (`cli_apply.py`)
You can also inspect and apply variants without a browser:

```bash
# List all assets and their available variants
python3 spycraft/tools/asset-harness/cli_apply.py list

# Apply a specific variant to an asset
python3 spycraft/tools/asset-harness/cli_apply.py apply sniper_rifle variant_a_barrett50
python3 spycraft/tools/asset-harness/cli_apply.py apply recon_drone variant_a_phantom_quad
python3 spycraft/tools/asset-harness/cli_apply.py apply c4_side variant_a_military_c4

# Apply an entire curated theme across all 20 mod textures in 1 command:
python3 spycraft/tools/asset-harness/cli_apply.py apply-preset specops
python3 spycraft/tools/asset-harness/cli_apply.py apply-preset cyberpunk
python3 spycraft/tools/asset-harness/cli_apply.py apply-preset steampunk
python3 spycraft/tools/asset-harness/cli_apply.py apply-preset baseline
```

---

## 2. Curated Aesthetic Themes

| Theme | Key Aesthetics | Color Palette | Best For |
|---|---|---|---|
| **SpecOps Tactical** | Matte black composite, military olive, green optics, carbon fiber | `#0f172a`, `#365314`, `#10b981`, `#64748b` | Modern military & covert stealth operations |
| **Cyberpunk Neon** | Titanium plating, glowing cyan energy coils, holographic HUD | `#0f172a`, `#06b6d4`, `#22d3ee`, `#ffffff` | High-tech sci-fi & futuristic espionage |
| **Steampunk / Vanilla** | Polished walnut wood, blued steel, riveted brass & copper gears | `#78350f`, `#b45309`, `#f59e0b`, `#334155` | Blending naturally with vanilla Minecraft survival |
| **Baseline Prototype** | Minimal geometric sprites from initial prototype phase | Mixed | Original reference |

---

## 3. Asset Catalog & Variations

| Asset | Category | Available Variants |
|---|---|---|
| `sniper_rifle` | Weapons | `current` (Baseline), `variant_a_barrett50` (SpecOps Barrett .50), `variant_b_covert_dmr` (Ghost Suppressed DMR), `variant_c_plasma_railgun` (Cyber Plasma Railgun), `variant_d_timberline_hunter` (Timberline Classic) |
| `recon_drone` | Surveillance | `current` (Baseline), `variant_a_phantom_quad` (Shadow Phantom Quad), `variant_b_stealth_predator` (Viper Stealth UAV), `variant_c_cyber_orb` (Aegis Recon Sphere), `variant_d_redstone_scout` (Clockwork Redstone Flyer) |
| `grappling_hook_gun` | Weapons | `current` (Baseline), `variant_a_tactical_winch` (Pneumatic Line Gun), `variant_b_crossbow_grapple` (Cross-Tether Launcher), `variant_c_plasma_harpoon` (Mag-Tether Harpoon), `variant_d_steampunk_grappler` (Steampunk Brass Winch) |
| `remote_detonator` | Explosives | `current` (Baseline), `variant_a_military_clamshell` (Pelican Rugged Clamshell), `variant_b_cyber_datapad` (Tactical Glass Datapad), `variant_c_hazard_plunger` (Industrial Blast Trigger), `variant_d_redstone_transmitter` (Arcane Redstone Activator) |
| `spy_tracker` | Surveillance | `current` (Baseline), `variant_a_crt_radar_pda` (Tactical CRT Radar), `variant_b_holo_compass` (Holographic Waypoint Scanner), `variant_c_amber_sonar` (Amber Sonar Gauge), `variant_d_tactical_tablet` (Military Topo Tablet) |
| `spy_goggles` | Surveillance | `current` (Baseline), `variant_a_pvs31_dual_nvg` (PVS-31 Dual NVG), `variant_b_cyber_hud_visor` (Cyber Neon HUD Visor), `variant_c_gpnvg_quad_panoramic` (GPNVG-18 Panoramic), `variant_d_steampunk_aviators` (Brass Clockwork Aviators) |
| `gps_navigator_goggles` | Surveillance | `current` (Baseline), `variant_a_pvs31_dual_nvg` (PVS-31 Cyan HUD), `variant_b_cyber_hud_visor` (Cyber Cyan Visor), `variant_c_gpnvg_quad_panoramic` (Panoramic Cyan Nav), `variant_d_steampunk_aviators` (Clockwork Sapphire Goggles) |
| `laser_remote` | Weapons | `current` (Baseline), `variant_a_laser_designator` (Tactical Laser Designator), `variant_b_security_keycard` (Security Access Keycard), `variant_c_field_transmitter` (Hazard Command Transmitter), `variant_d_cyber_remote` (Cyber Optical Remote) |
| `villager_disguise_mask` | Disguise | `current` (Baseline), `variant_a_carved_wooden_mask` (Carved Birch Mask), `variant_b_burlap_stitched_mask` (Stitched Burlap Face), `variant_c_porcelain_theatrical` (Porcelain Commedia Mask) |
| `villager_disguise_robe` | Disguise | `current` (Baseline), `variant_a_emerald_merchant_robe` (Emerald Merchant Garb), `variant_b_tactical_ghillie_cloak` (Tactical Ghillie Mantle), `variant_c_purple_cleric_robe` (Ceremonial Cleric Robe) |
| `c4_side` & `c4_top` | Explosives | `current` (Baseline), `variant_a_military_c4` (Military C4 Bricks & Tape), `variant_b_hazard_mining` (Hazard Mining Blast Block), `variant_c_cyber_nanite` (Cyber Nanite Demolition) |
| `sound_trap_front`, `side`, `top` | Explosives | `current` (Baseline), `variant_a_tactical_subwoofer` (Tactical Subwoofer & Flight Case), `variant_b_creeper_mimic` (Creeper Mimic Siren), `variant_c_cyber_sonic` (Cyber Sonic Projector) |
| `laser_pylon_base`, `post`, `emitter` | Explosives | `current` (Baseline), `variant_a_aegis_defense` (Tungsten Base, Redstone Post, Ruby Crystal), `variant_b_crying_obsidian` (Obsidian Runic Base, Netherite Post, Floating Prism) |
| `eriks_sword` & `eriks_star` | Weapons | `current` (Baseline), `variant_a_shadow_katana / shadow_ninja` (Shadow Katana & Shuriken), `variant_b_plasma_energy` (Plasma Saber & Glaive), `variant_c_royal_relic` (Royal Relic Broadsword & Gilded Star) |

---

## 4. Regenerating or Adding New Variants
The procedural pixel art generator is located at `spycraft/tools/asset-harness/generate_variants.py`.
To add new variants or modify color schemes, edit the 16x16 pattern matrices and run:
```bash
python3 spycraft/tools/asset-harness/generate_variants.py
```
This automatically updates the PNG files under `spycraft/tools/asset-harness/variants/`.
