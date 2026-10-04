#!/usr/bin/env python3
"""
SpyCraft Asset Variant CLI Switcher
Allows listing and applying texture variants directly from the command line.
"""

import os
import sys
import shutil

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
VARIANTS_DIR = os.path.join(BASE_DIR, "variants")
MOD_TEX_DIR = os.path.join(BASE_DIR, "../../src/main/resources/assets/spycraft/textures")

ASSET_MAP = {
    # Asset Key -> (Relative destination path in textures/, Type: item/block)
    "sniper_rifle": ("item/sniper_rifle.png", "item"),
    "recon_drone": ("item/recon_drone.png", "item"),
    "grappling_hook_gun": ("item/grappling_hook_gun.png", "item"),
    "remote_detonator": ("item/remote_detonator.png", "item"),
    "spy_tracker": ("item/spy_tracker.png", "item"),
    "spy_goggles": ("item/spy_goggles.png", "item"),
    "gps_navigator_goggles": ("item/gps_navigator_goggles.png", "item"),
    "laser_remote": ("item/laser_remote.png", "item"),
    "villager_disguise_mask": ("item/villager_disguise_mask.png", "item"),
    "villager_disguise_robe": ("item/villager_disguise_robe.png", "item"),
    "eriks_sword": ("item/eriks_sword.png", "item"),
    "eriks_star": ("item/eriks_star.png", "item"),
    "c4_side": ("block/c4_side.png", "block"),
    "c4_top": ("block/c4_top.png", "block"),
    "sound_trap_front": ("block/sound_trap_front.png", "block"),
    "sound_trap_side": ("block/sound_trap_side.png", "block"),
    "sound_trap_top": ("block/sound_trap_top.png", "block"),
    "laser_pylon_base": ("block/laser_pylon_base.png", "block"),
    "laser_pylon_post": ("block/laser_pylon_post.png", "block"),
    "laser_pylon_emitter": ("block/laser_pylon_emitter.png", "block"),
    "mission_beacon": ("item/mission_beacon.png", "item"),
    "thermal_goggles": ("item/thermal_goggles.png", "item"),
    "hologram_projector": ("item/hologram_projector.png", "item"),
    "tranquilizer_gun": ("item/tranquilizer_gun.png", "item"),
    "smoke_grenade": ("item/smoke_grenade.png", "item"),
    "climbing_gloves": ("item/climbing_gloves.png", "item"),
    "parachute_backpack": ("item/parachute_backpack.png", "item"),
    "binoculars": ("item/binoculars.png", "item"),
}

THEME_PRESETS = {
    "specops": {
        "name": "SpecOps Tactical (Matte Black & Military Olive)",
        "mapping": {
            "sniper_rifle": "variant_a_barrett50",
            "recon_drone": "variant_a_phantom_quad",
            "grappling_hook_gun": "variant_a_tactical_winch",
            "remote_detonator": "variant_a_military_clamshell",
            "spy_tracker": "variant_a_crt_radar_pda",
            "spy_goggles": "variant_a_pvs31_dual_nvg",
            "gps_navigator_goggles": "variant_a_pvs31_dual_nvg",
            "laser_remote": "variant_a_laser_designator",
            "villager_disguise_mask": "variant_a_carved_wooden_mask",
            "villager_disguise_robe": "variant_b_tactical_ghillie_cloak",
            "c4_side": "variant_a_military_c4",
            "c4_top": "variant_a_military_c4",
            "sound_trap_front": "variant_a_tactical_subwoofer",
            "sound_trap_side": "variant_a_tactical_subwoofer",
            "sound_trap_top": "variant_a_tactical_subwoofer",
            "laser_pylon_base": "variant_a_aegis_defense",
            "laser_pylon_post": "variant_a_aegis_defense",
            "laser_pylon_emitter": "variant_a_aegis_defense",
            "eriks_sword": "variant_a_shadow_katana",
            "eriks_star": "variant_a_shadow_ninja",
        }
    },
    "cyberpunk": {
        "name": "Cyberpunk Neon (Titanium & Cyan Plasma)",
        "mapping": {
            "sniper_rifle": "variant_c_plasma_railgun",
            "recon_drone": "variant_c_cyber_orb",
            "grappling_hook_gun": "variant_c_plasma_harpoon",
            "remote_detonator": "variant_b_cyber_datapad",
            "spy_tracker": "variant_b_holo_compass",
            "spy_goggles": "variant_b_cyber_hud_visor",
            "gps_navigator_goggles": "variant_b_cyber_hud_visor",
            "laser_remote": "variant_d_cyber_remote",
            "villager_disguise_mask": "variant_c_porcelain_theatrical",
            "villager_disguise_robe": "variant_c_purple_cleric_robe",
            "c4_side": "variant_c_cyber_nanite",
            "c4_top": "variant_c_cyber_nanite",
            "sound_trap_front": "variant_c_cyber_sonic",
            "sound_trap_side": "variant_c_cyber_sonic",
            "sound_trap_top": "variant_c_cyber_sonic",
            "laser_pylon_base": "variant_b_crying_obsidian",
            "laser_pylon_post": "variant_b_crying_obsidian",
            "laser_pylon_emitter": "variant_b_crying_obsidian",
            "eriks_sword": "variant_b_plasma_energy",
            "eriks_star": "variant_b_plasma_energy",
        }
    },
    "steampunk": {
        "name": "Steampunk / Vanilla Friendly (Brass, Wood & Copper)",
        "mapping": {
            "sniper_rifle": "variant_d_timberline_hunter",
            "recon_drone": "variant_d_redstone_scout",
            "grappling_hook_gun": "variant_d_steampunk_grappler",
            "remote_detonator": "variant_d_redstone_transmitter",
            "spy_tracker": "variant_c_amber_sonar",
            "spy_goggles": "variant_d_steampunk_aviators",
            "gps_navigator_goggles": "variant_d_steampunk_aviators",
            "laser_remote": "variant_c_field_transmitter",
            "villager_disguise_mask": "variant_a_carved_wooden_mask",
            "villager_disguise_robe": "variant_a_emerald_merchant_robe",
            "c4_side": "variant_b_hazard_mining",
            "c4_top": "variant_b_hazard_mining",
            "sound_trap_front": "variant_b_creeper_mimic",
            "sound_trap_side": "variant_b_creeper_mimic",
            "sound_trap_top": "variant_b_creeper_mimic",
            "laser_pylon_base": "variant_a_aegis_defense",
            "laser_pylon_post": "variant_a_aegis_defense",
            "laser_pylon_emitter": "variant_a_aegis_defense",
            "eriks_sword": "variant_c_royal_relic",
            "eriks_star": "variant_c_royal_relic",
        }
    },
    "baseline": {
        "name": "Original Baseline Textures",
        "mapping": {k: "current" for k in ASSET_MAP}
    }
}

def list_variants():
    print("=" * 65)
    print(" SpyCraft Assets & Available Variants")
    print("=" * 65)
    for asset, (rel_path, kind) in sorted(ASSET_MAP.items()):
        vdir = os.path.join(VARIANTS_DIR, asset)
        if not os.path.exists(vdir):
            continue
        variants = [os.path.splitext(f)[0] for f in sorted(os.listdir(vdir)) if f.endswith('.png')]
        print(f"[{kind.upper():5}] {asset}")
        for v in variants:
            print(f"        -> {v}")
    print("\nTheme Presets:")
    for key, p in THEME_PRESETS.items():
        print(f"  * {key:12} : {p['name']}")
    print("=" * 65)

def apply_variant(asset, variant):
    if asset not in ASSET_MAP:
        print(f"Error: Unknown asset '{asset}'. Run 'cli_apply.py list' to see all valid assets.")
        return False
    
    src_png = os.path.join(VARIANTS_DIR, asset, f"{variant}.png")
    if not os.path.exists(src_png):
        print(f"Error: Variant '{variant}' not found at {src_png}")
        return False
        
    rel_path, _ = ASSET_MAP[asset]
    dest_png = os.path.join(MOD_TEX_DIR, rel_path)
    os.makedirs(os.path.dirname(dest_png), exist_ok=True)
    shutil.copyfile(src_png, dest_png)
    print(f"Successfully applied '{variant}' to {rel_path}!")
    return True

def apply_preset(preset_key):
    if preset_key not in THEME_PRESETS:
        print(f"Error: Unknown preset '{preset_key}'. Choose from: {list(THEME_PRESETS.keys())}")
        return False
        
    preset = THEME_PRESETS[preset_key]
    print(f"Applying Theme Preset: {preset['name']}...")
    count = 0
    for asset, variant in preset["mapping"].items():
        if apply_variant(asset, variant):
            count += 1
    print(f"Finished applying preset '{preset_key}': {count} textures updated.")
    return True

if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] in ["--help", "-h", "help"]:
        print("Usage:")
        print("  python3 cli_apply.py list")
        print("  python3 cli_apply.py apply <asset> <variant>")
        print("  python3 cli_apply.py apply-preset <specops|cyberpunk|steampunk|baseline>")
        sys.exit(0)
        
    cmd = sys.argv[1].lower()
    if cmd == "list":
        list_variants()
    elif cmd == "apply":
        if len(sys.argv) < 4:
            print("Error: Missing arguments. Usage: python3 cli_apply.py apply <asset> <variant>")
            sys.exit(1)
        apply_variant(sys.argv[2], sys.argv[3])
    elif cmd == "apply-preset":
        if len(sys.argv) < 3:
            print("Error: Missing preset name. Usage: python3 cli_apply.py apply-preset <specops|cyberpunk|steampunk|baseline>")
            sys.exit(1)
        apply_preset(sys.argv[2])
    else:
        print(f"Unknown command '{cmd}'. Run with --help for usage.")
        sys.exit(1)
