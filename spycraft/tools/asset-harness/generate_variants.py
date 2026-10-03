#!/usr/bin/env python3
"""
SpyCraft Asset Variant Generator
Generates handcrafted, high-detail 16x16 pixel art textures for all SpyCraft assets.
Each asset gets 3-4 distinct artistic variants in addition to the current/baseline.
"""

import os
import shutil
from PIL import Image

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
VARIANTS_DIR = os.path.join(BASE_DIR, "variants")
MOD_TEX_DIR = os.path.join(BASE_DIR, "../../src/main/resources/assets/spycraft/textures")

os.makedirs(VARIANTS_DIR, exist_ok=True)

def hex_to_rgba(h, a=255):
    h = h.lstrip('#')
    if len(h) == 6:
        r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
        return (r, g, b, a)
    elif len(h) == 8:
        r, g, b, a = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), int(h[6:8], 16)
        return (r, g, b, a)
    raise ValueError(f"Invalid hex color: {h}")

def create_sprite(palette, pattern):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    resolved_palette = {}
    for k, v in palette.items():
        if isinstance(v, str):
            resolved_palette[k] = hex_to_rgba(v)
        else:
            resolved_palette[k] = v

    for y, row in enumerate(pattern):
        row = row.strip('\n')
        if len(row) != 16:
            raise ValueError(f"Pattern row {y} has length {len(row)} != 16: '{row}'")
        for x, char in enumerate(row):
            if char in resolved_palette:
                img.putpixel((x, y), resolved_palette[char])
            elif char == '.' or char == ' ':
                img.putpixel((x, y), (0, 0, 0, 0))
            else:
                raise ValueError(f"Unknown char '{char}' at ({x}, {y})")
    return img

def save_variant(asset_name, variant_name, img):
    target_dir = os.path.join(VARIANTS_DIR, asset_name)
    os.makedirs(target_dir, exist_ok=True)
    out_path = os.path.join(target_dir, f"{variant_name}.png")
    img.save(out_path, format="PNG")
    print(f"  [+] Saved {asset_name}/{variant_name}.png")

# =========================================================================
# 1. SNIPER RIFLE
# =========================================================================
def gen_sniper_rifle():
    print("Generating Sniper Rifle variants...")
    
    # Variant A: SpecOps Barrett .50 Anti-Materiel Rifle
    pal_a = {
        '#': '#0f172a', 'B': '#1e293b', 'M': '#334155', 'H': '#64748b',
        'S': '#059669', 's': '#34d399', 'G': '#eab308', 'P': '#475569', 'W': '#78350f',
    }
    pat_a = [
        "...............#",
        "..............##",
        "...........#MMB#",
        ".........#GGssS#",
        "........#MBsSS##",
        ".......#MMB##...",
        "......#MMB#.....",
        ".....#MMBP#.....",
        "....#MBBBP#.....",
        "...#MMMB##......",
        "..#WMMB#........",
        ".#WMBB#.........",
        ".#MBB#..........",
        "#MBB#...........",
        "##M#............",
        "................",
    ]
    save_variant("sniper_rifle", "variant_a_barrett50", create_sprite(pal_a, pat_a))

    # Variant B: Ghost Suppressed DMR
    pal_b = {
        '#': '#020617', 'D': '#0f172a', 'M': '#1e293b', 'L': '#334155',
        'R': '#ef4444', 'r': '#fca5a5', 'G': '#475569', 'C': '#64748b',
    }
    pat_b = [
        "...............#",
        ".............#DD",
        "............#MD#",
        "..........#rR#D#",
        ".........#RR#MD#",
        "........#MD##D#.",
        ".......#MD#.#D#.",
        "......#MDD##....",
        ".....#MDDD#.....",
        "....#MLDD#......",
        "...#MDDG#.......",
        "..#MDDD#........",
        "..#LD##.........",
        ".#MD#...........",
        ".##.............",
        "................",
    ]
    save_variant("sniper_rifle", "variant_b_covert_dmr", create_sprite(pal_b, pat_b))

    # Variant C: Cyber Plasma Railgun
    pal_c = {
        '#': '#030712', 'T': '#cbd5e1', 't': '#f8fafc', 'D': '#475569',
        'C': '#06b6d4', 'c': '#22d3ee', 'W': '#ffffff', 'B': '#3b82f6',
    }
    pat_c = [
        "...............#",
        ".............#Wc",
        "............#cCT",
        "..........#cW#cT",
        ".........#cc#CcT",
        "........#TT##CcT",
        ".......#TD#.#Cc#",
        "......#TTD##cT..",
        ".....#tTTD#B#...",
        "....#tTTDD#B#...",
        "...#tTD##.......",
        "..#tTDD#........",
        "..#TD##.........",
        ".#tTD#..........",
        ".##.............",
        "................",
    ]
    save_variant("sniper_rifle", "variant_c_plasma_railgun", create_sprite(pal_c, pat_c))

    # Variant D: Timberline Classic Sniper
    pal_d = {
        '#': '#1c1917', 'S': '#334155', 's': '#64748b', 'G': '#f59e0b',
        'g': '#fde047', 'L': '#0284c7', 'W': '#78350f', 'w': '#92400e', 'H': '#b45309',
    }
    pat_d = [
        "...............#",
        "..............#S",
        "...........#gsSS",
        ".........#GgLL#S",
        "........#SGL#gS#",
        ".......#SS##SS#.",
        "......#sSS##....",
        ".....#HwwSS#....",
        "....#HwwwSS#....",
        "...#HwwwwS#.....",
        "..#HwwwG#.......",
        ".#Hwwww#........",
        ".#Wwww#.........",
        "#Wwww#..........",
        "##W##...........",
        "................",
    ]
    save_variant("sniper_rifle", "variant_d_timberline_hunter", create_sprite(pal_d, pat_d))

# =========================================================================
# 2. RECON DRONE
# =========================================================================
def gen_recon_drone():
    print("Generating Recon Drone variants...")

    # Variant A: Shadow Phantom Quadcopter
    pal_a = {
        '#': '#09090b', 'C': '#18181b', 'M': '#27272a', 'H': '#52525b',
        'R': '#94a3b8', 'r': '#e2e8f0', 'L': '#06b6d4', 'l': '#67e8f9', 'E': '#ef4444',
    }
    pat_a = [
        ".rR#........#Rr.",
        "RrrR#......#RrrR",
        "#RrC#......#CrR#",
        ".#CMM#....#MMC#.",
        "..#CMM#..#MMC#..",
        "...#CHMMMMHC#...",
        "....#CMMEEMC#...",
        "...#HMMlLMMH#...",
        "...#CMMLLMMC#...",
        "...#CHMMMMHC#...",
        "..#CMM#..#MMC#..",
        ".#CMM#....#MMC#.",
        "#RrC#......#CrR#",
        "RrrR#......#RrrR",
        ".rR#........#Rr.",
        "................",
    ]
    save_variant("recon_drone", "variant_a_phantom_quad", create_sprite(pal_a, pat_a))

    # Variant B: Viper Stealth Drone
    pal_b = {
        '#': '#020617', 'D': '#0f172a', 'M': '#1e293b', 'H': '#334155',
        'G': '#f59e0b', 'g': '#fde047', 'F': '#ea580c', 'f': '#fbbf24',
    }
    pat_b = [
        ".......##.......",
        "......#gg#......",
        ".....#GGGG#.....",
        "....#MDGGDM#....",
        "...#MMDDDDMM#...",
        "...#HMDDDDMH#...",
        "..#MMHDDDDHMM#..",
        "..#MMMDDDDDMMM#.",
        ".#MMMDDDDDDDMMM#",
        ".#HMHDDDDDDDHMH#",
        "#MMMDDDDDDDDDMMM",
        "#MHDDF#DDD#FDDHM",
        "##D#Fff###ffF#D#",
        "...#Ff#...#fF#..",
        "....##.....##...",
        "................",
    ]
    save_variant("recon_drone", "variant_b_stealth_predator", create_sprite(pal_b, pat_b))

    # Variant C: Aegis Recon Sphere
    pal_c = {
        '#': '#030712', 'S': '#1e293b', 's': '#334155', 'H': '#64748b',
        'C': '#06b6d4', 'c': '#67e8f9', 'B': '#1d4ed8', 'b': '#38bdf8', 'W': '#ffffff',
    }
    pat_c = [
        "......####......",
        "....#sHHHHs#....",
        "...#sSHHHHSs#...",
        "..#sS#cc#cc#Ss#.",
        ".#sSCccccccCSs#.",
        ".#sCcc####ccCS#.",
        "#sCc#bBWbb#cCSs#",
        "#SHC#Bbbbb#CHSs#",
        "#SHC#bBBbb#CHSs#",
        "#sCc#bbbb##cCSs#",
        ".#sCcc####ccCS#.",
        ".#sSCccccccCSs#.",
        "..#sS#cc#cc#Ss#.",
        "...#sSHHHHSs#...",
        "....#sHHHHs#....",
        "......####......",
    ]
    save_variant("recon_drone", "variant_c_cyber_orb", create_sprite(pal_c, pat_c))

    # Variant D: Clockwork Redstone Flyer
    pal_d = {
        '#': '#1c1917', 'B': '#b45309', 'b': '#f59e0b', 'C': '#78350f',
        'R': '#dc2626', 'r': '#f87171', 'W': '#fef08a',
    }
    pat_d = [
        "CC##........##CC",
        ".CCW#......#WCC.",
        "..##B#....#B##..",
        "...#bBB##BBb#...",
        "...#BBbbbbBB#...",
        "..#Bb#rrrr#bB#..",
        "..#Bb#rRRr#bB#..",
        "..#Bb#RRRR#bB#..",
        "..#Bb#rRRr#bB#..",
        "..#Bb#rrrr#bB#..",
        "...#BBbbbbBB#...",
        "...#bBB##BBb#...",
        "..##B#....#B##..",
        ".CCW#......#WCC.",
        "CC##........##CC",
        "................",
    ]
    save_variant("recon_drone", "variant_d_redstone_scout", create_sprite(pal_d, pat_d))

# =========================================================================
# 3. GRAPPLING HOOK GUN
# =========================================================================
def gen_grappling_hook():
    print("Generating Grappling Hook Gun variants...")

    # Variant A: Pneumatic Line Gun
    pal_a = {
        '#': '#0f172a', 'B': '#1e293b', 'M': '#334155', 'H': '#64748b',
        'K': '#94a3b8', 'k': '#e2e8f0', 'G': '#ca8a04', 'S': '#cbd5e1', 's': '#ffffff',
    }
    pat_a = [
        ".............#k.",
        "............#kK#",
        "...........#kK#.",
        "..........#KKK#.",
        "........#HMKB#..",
        ".......#HMBGG#..",
        "......#HMBGG##..",
        ".....#HMBB#Ss#..",
        "....#HMBB#SSS#..",
        "...#HMBB##SS##..",
        "..#HMB#.........",
        ".#HMB#..........",
        "#HMB#...........",
        "##M#............",
        "................",
        "................",
    ]
    save_variant("grappling_hook_gun", "variant_a_tactical_winch", create_sprite(pal_a, pat_a))

    # Variant B: Cross-Tether Harpoon
    pal_b = {
        '#': '#020617', 'L': '#475569', 'l': '#94a3b8', 'B': '#1e293b',
        'S': '#e2e8f0', 'H': '#f87171', 'h': '#ef4444', 'W': '#78350f',
    }
    pat_b = [
        "..............#l",
        "............#hL#",
        "...........#hH#S",
        "..........#HBB#S",
        "........#BLBB#S.",
        ".......#BBLBB#S.",
        "......#BBBBL#S..",
        ".....#BBBBB#S...",
        "....#BBBBB#l....",
        "...#WBBB##......",
        "..#WBB#.........",
        ".#WBB#..........",
        "#WBB#...........",
        "##B#............",
        "................",
        "................",
    ]
    save_variant("grappling_hook_gun", "variant_b_crossbow_grapple", create_sprite(pal_b, pat_b))

    # Variant C: Mag-Tether Harpoon
    pal_c = {
        '#': '#030712', 'D': '#0f172a', 'C': '#06b6d4', 'c': '#22d3ee',
        'T': '#cbd5e1', 't': '#ffffff', 'Y': '#eab308', 'y': '#ca8a04',
    }
    pat_c = [
        "..............#t",
        ".............#Tt",
        "............#TT#",
        "..........#ccTT#",
        ".........#cCDD#.",
        "........#cCDD#..",
        ".......#CCDD#...",
        "......#YyDD#....",
        ".....#YyDDD#....",
        "....#DCDDD#.....",
        "...#DDDD##......",
        "..#DDDD#........",
        ".#DCD##.........",
        "#DD#............",
        "##..............",
        "................",
    ]
    save_variant("grappling_hook_gun", "variant_c_plasma_harpoon", create_sprite(pal_c, pat_c))

    # Variant D: Steampunk Brass Winch
    pal_d = {
        '#': '#1c1917', 'B': '#d97706', 'b': '#f59e0b', 'I': '#374151',
        'i': '#9ca3af', 'C': '#b45309', 'G': '#fde047', 'W': '#78350f',
    }
    pat_d = [
        "............#i..",
        "...........#iI#.",
        "..........#iII#.",
        ".........#IIII#.",
        ".......#bBII##..",
        "......#bBCCG#...",
        ".....#bBCCGG#...",
        "....#bBCBB#.....",
        "...#bBBB##......",
        "..#WBBB#........",
        ".#WWB#..........",
        "#WWB#...........",
        "#W##............",
        "##..............",
        "................",
        "................",
    ]
    save_variant("grappling_hook_gun", "variant_d_steampunk_grappler", create_sprite(pal_d, pat_d))

# =========================================================================
# 4. REMOTE DETONATOR
# =========================================================================
def gen_remote_detonator():
    print("Generating Remote Detonator variants...")

    # Variant A: Pelican Rugged Detonator
    pal_a = {
        '#': '#052e16', 'O': '#365314', 'o': '#4d7c0f', 'D': '#18181b',
        'G': '#22c55e', 'g': '#86efac', 'R': '#dc2626', 'r': '#ef4444',
        'A': '#0f172a', 'a': '#334155', 'k': '#eab308',
    }
    pat_a = [
        "...#a#..........",
        "...#A#..........",
        "...#A#..........",
        "..##A###........",
        ".#oooooo#.......",
        "#oOOOOOOo#......",
        "#oODDDDOo#......",
        "#oDGggGDo#......",
        "#oODDDDOo#......",
        "#oORrrROo#......",
        "#oORRkROo#......",
        "#oOOOOOOo#......",
        "#oODDDDOo#......",
        "#oOOOOOOo#......",
        ".#oooooo#.......",
        "..######........",
    ]
    save_variant("remote_detonator", "variant_a_military_clamshell", create_sprite(pal_a, pat_a))

    # Variant B: Tactical Glass Datapad
    pal_b = {
        '#': '#020617', 'F': '#0f172a', 'f': '#1e293b', 'C': '#06b6d4',
        'c': '#22d3ee', 'W': '#e0f2fe', 'Y': '#f59e0b', 'R': '#ef4444', 'r': '#fca5a5',
    }
    pat_b = [
        "..########......",
        ".#ffffffff#.....",
        "#fFCCCCCCFf#....",
        "#fFcWccWcCF#....",
        "#fFCCCCCCFf#....",
        "#fFcYYYYcCF#....",
        "#fFcYYYYcCF#....",
        "#fFcYYYYcCF#....",
        "#fFCCCCCCFf#....",
        "#fFcRrrRccF#....",
        "#fFcRRRRccF#....",
        "#fFCCCCCCFf#....",
        "#fFffffffFf#....",
        ".#ffffffff#.....",
        "..########......",
        "................",
    ]
    save_variant("remote_detonator", "variant_b_cyber_datapad", create_sprite(pal_b, pat_b))

    # Variant C: Industrial Blast Trigger
    pal_c = {
        '#': '#1c1917', 'Y': '#eab308', 'y': '#ca8a04', 'K': '#18181b',
        'R': '#dc2626', 'r': '#ef4444', 'S': '#9ca3af', 'D': '#475569', 'd': '#f87171',
    }
    pat_c = [
        "....#rrrr#......",
        "...#rRRRRr#.....",
        "...#rRRRRr#.....",
        "....#SSSS#......",
        "..############..",
        ".#YyKYyKYyKYyK#.",
        "#YyKYyKYyKYyKYy#",
        "#K#y#dd##y#K#yK#",
        "#yKYy#DD#yKYyKY#",
        "#KYyKY##YyKYyKY#",
        "#YyKYyKYyKYyKYy#",
        "#KYyKYyKYyKYyKY#",
        "#YyKYyKYyKYyKYy#",
        ".#KYyKYyKYyKY#..",
        "..##########....",
        "................",
    ]
    save_variant("remote_detonator", "variant_c_hazard_plunger", create_sprite(pal_c, pat_c))

    # Variant D: Arcane Redstone Activator
    pal_d = {
        '#': '#0f172a', 'D': '#334155', 'd': '#475569', 'R': '#dc2626',
        'r': '#ef4444', 'W': '#fca5a5', 'G': '#f59e0b', 'g': '#fde047',
    }
    pat_d = [
        ".....#WW#.......",
        "....#WrrW#......",
        "....#rrrr#......",
        "....#rRRr#......",
        "..###dDDd###....",
        ".#dddddddddd#...",
        "#ddDDDDDDDDdd#..",
        "#dDDGggggGDDd#..",
        "#dDDGggggGDDd#..",
        "#dDDDDDDDDDDd#..",
        "#dDD#rrrr#DDd#..",
        "#dDD#rRRr#DDd#..",
        "#dDDDDDDDDDDd#..",
        ".#dddddddddd#...",
        "..##########....",
        "................",
    ]
    save_variant("remote_detonator", "variant_d_redstone_transmitter", create_sprite(pal_d, pat_d))

# =========================================================================
# 5. SPY TRACKER
# =========================================================================
def gen_spy_tracker():
    print("Generating Spy Tracker variants...")

    # Variant A: Tactical CRT Radar
    pal_a = {
        '#': '#020617', 'B': '#1e293b', 'b': '#334155', 'G': '#052e16',
        'g': '#15803d', 'S': '#22c55e', 'E': '#86efac', 'R': '#ef4444', 'A': '#475569',
    }
    pat_a = [
        "....#AA#........",
        "....#AA#........",
        "..###bb###......",
        ".#bbbbbbbb#.....",
        "#bBBBBBBBBb#....",
        "#bB#gggg#Bb#....",
        "#bBgGSSGgBb#....",
        "#bBgSEEGgBb#....",
        "#bBgGGGRgBb#....",
        "#bB#gggg#Bb#....",
        "#bBBBBBBBBb#....",
        "#bB##..##Bb#....",
        "#bBBBBBBBBb#....",
        ".#bbbbbbbb#.....",
        "..########......",
        "................",
    ]
    save_variant("spy_tracker", "variant_a_crt_radar_pda", create_sprite(pal_a, pat_a))

    # Variant B: Holographic Waypoint Scanner
    pal_b = {
        '#': '#030712', 'T': '#64748b', 't': '#94a3b8', 'C': '#0891b2',
        'c': '#06b6d4', 'H': '#22d3ee', 'W': '#ffffff',
    }
    pat_b = [
        "......####......",
        "....#tTTTTt#....",
        "...#tTCCCCtT#...",
        "..#tTc####cTt#..",
        ".#tTC#cHHc#CTt#.",
        ".#TTC#cHHc#CTT#.",
        "#tTCc##HH##cCTt#",
        "#TTCccccHccccTT#",
        "#TTCcc#####ccTT#",
        "#tTCc###W##cCTt#",
        ".#TTC#cccc#CTT#.",
        ".#tTC######CTt#.",
        "..#tTc####cTt#..",
        "...#tTCCCCtT#...",
        "....#tTTTTt#....",
        "......####......",
    ]
    save_variant("spy_tracker", "variant_b_holo_compass", create_sprite(pal_b, pat_b))

    # Variant C: Amber Sonar Gauge
    pal_c = {
        '#': '#1c1917', 'B': '#d97706', 'b': '#f59e0b', 'A': '#451a03',
        'a': '#b45309', 'N': '#fde047', 'P': '#ef4444',
    }
    pat_c = [
        "......####......",
        "....#bBBBBb#....",
        "...#bBAAAAbB#...",
        "..#bBa####aBb#..",
        ".#bBAaNN##aABb#.",
        ".#BBAa#NN#aABB#.",
        "#bBAa##NN#aABBb#",
        "#BBAaa##N#aAABB#",
        "#BBAaaa#P#aAABB#",
        "#bBAa####aABBb#.",
        ".#BBAaaaaaABB#..",
        ".#bBAAAAAABb#...",
        "..#bBa##aBb#....",
        "...#bBBBBb#.....",
        "....######......",
        "................",
    ]
    save_variant("spy_tracker", "variant_c_amber_sonar", create_sprite(pal_c, pat_c))

    # Variant D: Military Topo Tablet
    pal_d = {
        '#': '#020617', 'C': '#0f172a', 'c': '#1e293b', 'M': '#14532d',
        'L': '#16a34a', 'P': '#ef4444', 'T': '#22c55e', 'B': '#38bdf8',
    }
    pat_d = [
        "###############.",
        "#ccccccccccccc#.",
        "#cBB#MMLLMMMMc#.",
        "#c##MMLLMMMMMc#.",
        "#cMMMMLMMMTMMc#.",
        "#cMMMMLMMTTMMc#.",
        "#cLLMMLLLLMMMc#.",
        "#cLLMMMMMMMMMc#.",
        "#cMMMMMPPMMMMc#.",
        "#cMMMMMPPMMMMc#.",
        "#cMMLLMMMMLLMc#.",
        "#cMLLLMMMMLLMc#.",
        "#cMMMMMMMMMMMc#.",
        "#ccccccccccccc#.",
        "###############.",
        "................",
    ]
    save_variant("spy_tracker", "variant_d_tactical_tablet", create_sprite(pal_d, pat_d))

# =========================================================================
# 6. SPY GOGGLES & GPS GOGGLES
# =========================================================================
def gen_goggles():
    print("Generating Spy Goggles & GPS Goggles variants...")

    # Spy Goggles (Green NVG):
    # Variant A: PVS-31 Tactical Dual NVG
    pal_a = {
        '#': '#020617', 'B': '#1e293b', 'b': '#334155', 'G': '#059669',
        'g': '#10b981', 'W': '#a7f3d0', 'S': '#475569',
    }
    pat_a = [
        "................",
        "...##......##...",
        "..#SS######SS#..",
        ".#SSSSSSSSSSSS#.",
        "#bb######bb####.",
        "#bB#gWgg#bB#gWgg",
        "#bB#gggg#bB#gggg",
        "#bB#gggg#bB#gggg",
        "#bb######bb####.",
        ".#SS#......#SS#.",
        "..##........##..",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    save_variant("spy_goggles", "variant_a_pvs31_dual_nvg", create_sprite(pal_a, pat_a))

    # Variant B: Cyber Neon HUD Visor
    pal_b = {
        '#': '#030712', 'F': '#0f172a', 'f': '#1e293b', 'C': '#059669',
        'c': '#34d399', 'W': '#ecfdf5',
    }
    pat_b = [
        "................",
        "..############..",
        ".#ffffffffffff#.",
        "#ffffffffffffff#",
        "#fFccccccccccFf#",
        "#FCccWccccWccCF#",
        "#FCccccccccccCF#",
        "#fFccccccccccFf#",
        ".#ffffffffffff#.",
        "..############..",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    save_variant("spy_goggles", "variant_b_cyber_hud_visor", create_sprite(pal_b, pat_b))

    # Variant C: GPNVG-18 Panoramic NVG (4 tubes!)
    pal_c = {
        '#': '#0c0a09', 'B': '#292524', 'b': '#44403c', 'G': '#15803d',
        'g': '#22c55e', 'S': '#78716c',
    }
    pat_c = [
        "................",
        ".....######.....",
        "..####SSSS####..",
        ".#bbbbbbbbbbbb#.",
        "#B#gg#B#gg#B#gg#",
        "#B#gg#B#gg#B#gg#",
        "#bb##bb##bb##bb#",
        ".##..##..##..##.",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    save_variant("spy_goggles", "variant_c_gpnvg_quad_panoramic", create_sprite(pal_c, pat_c))

    # Variant D: Brass Clockwork Aviators
    pal_d = {
        '#': '#1c1917', 'L': '#78350f', 'B': '#d97706', 'b': '#f59e0b',
        'G': '#047857', 'g': '#10b981',
    }
    pat_d = [
        "................",
        "...##......##...",
        "..#LL######LL#..",
        ".#LLLLLLLLLLLL#.",
        "#bb######bb####.",
        "#bB#gGgg#bB#gGgg",
        "#bB#GGGG#bB#GGGG",
        "#bb######bb####.",
        ".#LL#......#LL#.",
        "..##........##..",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    save_variant("spy_goggles", "variant_d_steampunk_aviators", create_sprite(pal_d, pat_d))

    # GPS Navigator Goggles:
    pal_gps_a = {
        '#': '#020617', 'B': '#1e293b', 'b': '#334155', 'G': '#0284c7',
        'g': '#38bdf8', 'W': '#e0f2fe', 'S': '#475569',
    }
    save_variant("gps_navigator_goggles", "variant_a_pvs31_dual_nvg", create_sprite(pal_gps_a, pat_a))

    pal_gps_b = {
        '#': '#030712', 'F': '#0f172a', 'f': '#1e293b', 'C': '#0891b2',
        'c': '#06b6d4', 'W': '#ffffff',
    }
    save_variant("gps_navigator_goggles", "variant_b_cyber_hud_visor", create_sprite(pal_gps_b, pat_b))

    pal_gps_c = {
        '#': '#0c0a09', 'B': '#292524', 'b': '#44403c', 'G': '#0369a1',
        'g': '#38bdf8', 'S': '#78716c',
    }
    save_variant("gps_navigator_goggles", "variant_c_gpnvg_quad_panoramic", create_sprite(pal_gps_c, pat_c))

    pal_gps_d = {
        '#': '#1c1917', 'L': '#78350f', 'B': '#d97706', 'b': '#f59e0b',
        'G': '#0284c7', 'g': '#38bdf8',
    }
    save_variant("gps_navigator_goggles", "variant_d_steampunk_aviators", create_sprite(pal_gps_d, pat_d))

# =========================================================================
# 7. LASER REMOTE
# =========================================================================
def gen_laser_remote():
    print("Generating Laser Remote variants...")

    # Variant A: Tactical Laser Designator
    pal_a = {
        '#': '#020617', 'B': '#1e293b', 'b': '#334155', 'R': '#dc2626',
        'r': '#ef4444', 'W': '#ffffff', 'A': '#475569',
    }
    pat_a = [
        "....#W#.........",
        "...#rRr#........",
        "..##RRR##.......",
        ".#bbbbbbb#......",
        "#bBBBBBBBb#.....",
        "#bB#rRr#Bb#.....",
        "#bBBBBBBBb#.....",
        "#bB#BBB#Bb#.....",
        "#bBBBBBBBb#.....",
        "#bB#####Bb#.....",
        "#bBBBBBBBb#.....",
        ".#bbbbbbb#......",
        "..#######.......",
        "................",
        "................",
        "................",
    ]
    save_variant("laser_remote", "variant_a_laser_designator", create_sprite(pal_a, pat_a))

    # Variant B: Security Keycard
    pal_b = {
        '#': '#030712', 'F': '#0f172a', 'f': '#334155', 'C': '#eab308',
        'c': '#fde047', 'R': '#dc2626', 'r': '#f87171',
    }
    pat_b = [
        "..############..",
        ".#ffffffffffff#.",
        "#fFFFFFFFFFFFFf#",
        "#fF#Cc#.#rRr#Ff#",
        "#fF#cC#.#RRR#Ff#",
        "#fFFFFFFFFFFFFf#",
        "#fF##########Ff#",
        "#fFFFFFFFFFFFFf#",
        "#fF#r..r..r#Ff#.",
        "#fFFFFFFFFFFFFf#",
        ".#ffffffffffff#.",
        "..############..",
        "................",
        "................",
        "................",
        "................",
    ]
    save_variant("laser_remote", "variant_b_security_keycard", create_sprite(pal_b, pat_b))

    # Variant C: Industrial Field Transmitter
    pal_c = {
        '#': '#18181b', 'Y': '#eab308', 'y': '#ca8a04', 'K': '#27272a',
        'R': '#ef4444', 'r': '#f87171', 'T': '#94a3b8',
    }
    pat_c = [
        "....#TT#........",
        "....#TT#........",
        "..##yYYy##......",
        ".#yYYYYYYy#.....",
        "#yYKKKKKKYy#....",
        "#yYK#rr#KYy#....",
        "#yYK#RR#KYy#....",
        "#yYKKKKKKYy#....",
        "#yYK#TT#KYy#....",
        "#yYKKKKKKYy#....",
        "#yYYYYYYYy#.....",
        ".#yYYYYYYy#.....",
        "..########......",
        "................",
        "................",
        "................",
    ]
    save_variant("laser_remote", "variant_c_field_transmitter", create_sprite(pal_c, pat_c))

    # Variant D: Cyber Optical Remote
    pal_d = {
        '#': '#030712', 'C': '#0f172a', 'c': '#1e293b', 'L': '#06b6d4',
        'l': '#67e8f9', 'W': '#ffffff',
    }
    pat_d = [
        "....#W#.........",
        "...#lLl#........",
        "..#lLLLl#.......",
        ".#ccCCCCcc#.....",
        "#cCC#ll#CCc#....",
        "#cC#lWll#Cc#....",
        "#cCC#ll#CCc#....",
        "#cCCCCCCCCc#....",
        "#cCC#LL#CCc#....",
        "#cCCCCCCCCc#....",
        ".#ccCCCCcc#.....",
        "..########......",
        "................",
        "................",
        "................",
        "................",
    ]
    save_variant("laser_remote", "variant_d_cyber_remote", create_sprite(pal_d, pat_d))

# =========================================================================
# 8. VILLAGER DISGUISE MASK & ROBE
# =========================================================================
def gen_villager_disguise():
    print("Generating Villager Disguise variants...")

    # Mask Variant A: Hand-Carved Birch Villager Mask
    pal_a = {
        '#': '#292524', 'W': '#d6d3d1', 'w': '#a8a29e', 'B': '#44403c',
        'N': '#78716c', 'n': '#57534e', 'E': '#020617', 'S': '#78350f',
    }
    pat_a = [
        "....########....",
        "...#WwwwwwwW#...",
        "..#W#BBBBBB#W#..",
        ".#SW#B#EE#B#WS#.",
        "#SWW#w#EE#w#WWS#",
        "#SWWwwwwwwwwWWS#",
        ".#WWWW#NN#WWWW#.",
        "..#WWW#NN#WWW#..",
        "..#Www#NN#wwW#..",
        "...#Ww#nn#wW#...",
        "...#WWwwwwWW#...",
        "....#WWwwWW#....",
        ".....######.....",
        "................",
        "................",
        "................",
    ]
    save_variant("villager_disguise_mask", "variant_a_carved_wooden_mask", create_sprite(pal_a, pat_a))

    # Mask Variant B: Stitched Burlap Face
    pal_b = {
        '#': '#451a03', 'W': '#a16207', 'w': '#713f12', 'S': '#451a03',
        'E': '#000000', 'N': '#854d0e',
    }
    pat_b = [
        "....########....",
        "...#WwWwWwWw#...",
        "..#WSWWWWWSW#...",
        ".#WWE#EE#EWWS#..",
        "#WWE#EEEE#EWWS#.",
        "#WWWWSSSSWWWWWS#",
        ".#WWWW#NN#WWWW#.",
        "..#WWW#NN#WWW#..",
        "...#WW#NN#WW#...",
        "...#WW#ww#WW#...",
        "....#WWWWWW#....",
        ".....######.....",
        "................",
        "................",
        "................",
        "................",
    ]
    save_variant("villager_disguise_mask", "variant_b_burlap_stitched_mask", create_sprite(pal_b, pat_b))

    # Mask Variant C: Porcelain Commedia Mask
    pal_c = {
        '#': '#1e293b', 'P': '#f8fafc', 'p': '#cbd5e1', 'E': '#020617',
        'G': '#10b981', 'g': '#34d399', 'N': '#94a3b8',
    }
    pat_c = [
        "....########....",
        "...#PppppppP#...",
        "..#P#pppppp#P#..",
        ".#pP#P#EE#P#Pp#.",
        "#pPP#g#EE#g#PPp#",
        "#pPPgg#pp#ggPPp#",
        ".#PPPP#NN#PPPP#.",
        "..#PPP#NN#PPP#..",
        "...#PP#NN#PP#...",
        "...#PP#NN#PP#...",
        "....#PPPPPP#....",
        ".....######.....",
        "................",
        "................",
        "................",
        "................",
    ]
    save_variant("villager_disguise_mask", "variant_c_porcelain_theatrical", create_sprite(pal_c, pat_c))

    # Robe Variant A: Emerald Merchant Robe
    pal_r_a = {
        '#': '#292524', 'R': '#7c2d12', 'r': '#9a3412', 'D': '#431407',
        'E': '#10b981', 'e': '#34d399', 'G': '#eab308',
    }
    pat_r_a = [
        "......####......",
        "....#rRRRRr#....",
        "...#rRDDDDRr#...",
        "...#rR#EE#Rr#...",
        "..#rrR#eE#Rrr#..",
        ".#rRRDDGGDDDrr#.",
        "#rRDDRRRRRRDDDr#",
        "#rDDRRRRRRRRDDr#",
        "#rDDRRRRRRRRDDr#",
        "#rDDRRDDDDRRDDr#",
        "#rDDRRDDDDRRDDr#",
        "#rDDRRDDDDRRDDr#",
        ".#rDRRDDDDRDr#..",
        "..#rRDDDDDDr#...",
        "...########.....",
        "................",
    ]
    save_variant("villager_disguise_robe", "variant_a_emerald_merchant_robe", create_sprite(pal_r_a, pat_r_a))

    # Robe Variant B: Tactical Ghillie Mantle
    pal_r_b = {
        '#': '#0f172a', 'G': '#1e293b', 'g': '#334155', 'D': '#020617',
        'C': '#365314', 'c': '#4d7c0f',
    }
    pat_r_b = [
        "......####......",
        "....#gGGGGg#....",
        "...#gGDDDDCg#...",
        "...#gC#DD#Cc#...",
        "..#cgG#DD#Ggc#..",
        ".#cGGDDDDDDGgc#.",
        "#gCDDGGGGGGDDcg#",
        "#cDDGGccGGGGDDc#",
        "#cDDGcCCCGGGDDc#",
        "#gDDGgDDDDGGDDg#",
        "#gDDGgDDDDGGDDg#",
        "#cDDGgDDDDGGDDc#",
        ".#gCGgDDDDGcgc#.",
        "..#gCGDDDDGcg#..",
        "...########.....",
        "................",
    ]
    save_variant("villager_disguise_robe", "variant_b_tactical_ghillie_cloak", create_sprite(pal_r_b, pat_r_b))

    # Robe Variant C: Cleric Ceremonial Vestment
    pal_r_c = {
        '#': '#1e1b4b', 'P': '#581c87', 'p': '#7e22ce', 'D': '#3b0764',
        'G': '#eab308', 'g': '#fde047',
    }
    pat_r_c = [
        "......####......",
        "....#pPPPPp#....",
        "...#pPDDDDPp#...",
        "...#pP#GG#Pp#...",
        "..#ppP#gG#Ppp#..",
        ".#pPPDDggDDPpp#.",
        "#pPDDPPPPPPDDDp#",
        "#pDDPPPggPPPDDp#",
        "#pDDPPPggPPPDDp#",
        "#pDDPPDDDDPPDDp#",
        "#pDDPPDggDPPDDp#",
        "#pDDPPDggDPPDDp#",
        ".#pDPPDggDPDp#..",
        "..#pPDDDDDDDp#..",
        "...########.....",
        "................",
    ]
    save_variant("villager_disguise_robe", "variant_c_purple_cleric_robe", create_sprite(pal_r_c, pat_r_c))

# =========================================================================
# 9. C4 EXPLOSIVE BLOCK
# =========================================================================
def gen_c4_block():
    print("Generating C4 Explosive Block variants...")

    # Variant A: Military C4 Demolition Charge
    pal_side_a = {
        '#': '#14532d', 'O': '#365314', 'o': '#4d7c0f', 'T': '#1c1917',
        't': '#44403c', 'D': '#09090b', 'R': '#ef4444', 'r': '#f87171',
        'B': '#2563eb', 'b': '#60a5fa', 'S': '#9ca3af',
    }
    pat_side_a = [
        "TTTTTTTTTTTTTTTT",
        "t#oooooooooooo#t",
        "t#oOOOOOOOOOOo#t",
        "t#oO#S#OO#S#Oo#t",
        "TTTT#B#TT#R#TTTT",
        "o#oo#b#oo#r#ooo#",
        "O#oODDDDDDDo#Oo#",
        "O#oODrRRrRDo#Oo#",
        "O#oODDDDDDDo#Oo#",
        "TTTTTTTTTTTTTTTT",
        "o#oooooooooooo#o",
        "O#oOOOOOOOOOOo#O",
        "O#oOOOOOOOOOOo#O",
        "TTTTTTTTTTTTTTTT",
        "O#oOOOOOOOOOOo#O",
        "################",
    ]
    save_variant("c4_side", "variant_a_military_c4", create_sprite(pal_side_a, pat_side_a))

    pal_top_a = {
        '#': '#14532d', 'O': '#365314', 'o': '#4d7c0f', 'T': '#1c1917',
        't': '#44403c', 'S': '#9ca3af', 'R': '#ef4444', 'A': '#020617',
    }
    pat_top_a = [
        "TTTTTTTTTTTTTTTT",
        "T#oooooTT#oooooT",
        "T#oOOOoTT#oOOOoT",
        "T#oO#SOtT#oO#SOt",
        "T#oOOOoTT#oOOOoT",
        "TTTTTTTTTTTTTTTT",
        "TTTTTT#AA#TTTTTT",
        "TTTTTT#AA#TTTTTT",
        "TTTTTTTTTTTTTTTT",
        "T#oOOOoTT#oOOOoT",
        "T#oO#ROtT#oOOOoT",
        "T#oOOOoTT#oOOOoT",
        "T#oooooTT#oooooT",
        "TTTTTTTTTTTTTTTT",
        "T#oooooTT#oooooT",
        "TTTTTTTTTTTTTTTT",
    ]
    save_variant("c4_top", "variant_a_military_c4", create_sprite(pal_top_a, pat_top_a))

    # Variant B: Hazard Mining Blast Block
    pal_side_b = {
        '#': '#18181b', 'Y': '#eab308', 'y': '#ca8a04', 'K': '#27272a',
        'R': '#dc2626', 'r': '#ef4444', 'S': '#475569', 's': '#64748b',
    }
    pat_side_b = [
        "################",
        "#yYKYyKYyKYyKYy#",
        "#YyKYyKYyKYyKYy#",
        "#sSSSSSSSSSSSSs#",
        "#S#y#rrrrrr#y#S#",
        "#S#Y#rRRRRr#Y#S#",
        "#S#y#rRRRRr#y#S#",
        "#S#Y#rrrrrr#Y#S#",
        "#sSSSSSSSSSSSSs#",
        "#YyKYyKYyKYyKYy#",
        "#yYKYyKYyKYyKYy#",
        "#sSSSSSSSSSSSSs#",
        "#S############S#",
        "#sSSSSSSSSSSSSs#",
        "#YyKYyKYyKYyKYy#",
        "################",
    ]
    save_variant("c4_side", "variant_b_hazard_mining", create_sprite(pal_side_b, pat_side_b))

    pal_top_b = {
        '#': '#18181b', 'S': '#475569', 's': '#64748b', 'B': '#94a3b8',
        'Y': '#eab308', 'R': '#ef4444',
    }
    pat_top_b = [
        "################",
        "#sSSSSSSSSSSSSs#",
        "#S#B#SSSSSS#B#S#",
        "#SS#SSSSSSSS#SS#",
        "#SS#s######s#SS#",
        "#SSS#Y#RR#Y#SSS#",
        "#SSS#RRRRRR#SSS#",
        "#SSS#RRRRRR#SSS#",
        "#SSS#Y#RR#Y#SSS#",
        "#SS#s######s#SS#",
        "#SS#SSSSSSSS#SS#",
        "#S#B#SSSSSS#B#S#",
        "#sSSSSSSSSSSSSs#",
        "################",
        "################",
        "################",
    ]
    save_variant("c4_top", "variant_b_hazard_mining", create_sprite(pal_top_b, pat_top_b))

    # Variant C: Nanite Plasma Charge
    pal_side_c = {
        '#': '#020617', 'C': '#0f172a', 'c': '#1e293b', 'L': '#06b6d4',
        'l': '#67e8f9', 'W': '#ffffff', 'G': '#334155',
    }
    pat_side_c = [
        "################",
        "#cCCCCCCCCCCCCc#",
        "#C#GG#GG#GG#G#C#",
        "#C#ll#ll#ll#l#C#",
        "#C#lLLLLLLLLl#C#",
        "#C#lLWWllWWLl#C#",
        "#C#lLLLLLLLLl#C#",
        "#cCCCCCCCCCCCCc#",
        "#cCCCCCCCCCCCCc#",
        "#C#lLLLLLLLLl#C#",
        "#C#lLWWllWWLl#C#",
        "#C#lLLLLLLLLl#C#",
        "#C#ll#ll#ll#l#C#",
        "#C#GG#GG#GG#G#C#",
        "#cCCCCCCCCCCCCc#",
        "################",
    ]
    save_variant("c4_side", "variant_c_cyber_nanite", create_sprite(pal_side_c, pat_side_c))

    pal_top_c = {
        '#': '#020617', 'C': '#0f172a', 'c': '#1e293b', 'L': '#06b6d4',
        'l': '#67e8f9', 'W': '#ffffff',
    }
    pat_top_c = [
        "################",
        "#cCCCCCCCCCCCCc#",
        "#C#ll######ll#C#",
        "#C#llLLLLLLll#C#",
        "#C#lLLCCCCLLl#C#",
        "#C#LCC#ll#CCL#C#",
        "#C#LC#lWWl#CL#C#",
        "#C#LC#lWWl#CL#C#",
        "#C#LCC#ll#CCL#C#",
        "#C#lLLCCCCLLl#C#",
        "#C#llLLLLLLll#C#",
        "#C#ll######ll#C#",
        "#cCCCCCCCCCCCCc#",
        "################",
        "################",
        "################",
    ]
    save_variant("c4_top", "variant_c_cyber_nanite", create_sprite(pal_top_c, pat_top_c))

# =========================================================================
# 10. SOUND TRAP BLOCK
# =========================================================================
def gen_sound_trap():
    print("Generating Sound Trap variants...")

    # Variant A: Tactical Subwoofer Lure
    pal_front_a = {
        '#': '#020617', 'F': '#1e293b', 'f': '#334155', 'G': '#09090b',
        'g': '#18181b', 'C': '#27272a', 'c': '#3f3f46', 'D': '#52525b',
        'd': '#71717a', 'R': '#ef4444',
    }
    pat_front_a = [
        "##ffffffffffff##",
        "#fFFFFFFFFFFFFf#",
        "#fF#R#gGgGgG#Ff#",
        "#fFgg#cccc#ggFf#",
        "#fFg#cCCCCc#gFf#",
        "#fG#cCCCCCCc#Gf#",
        "#fG#cCDddDCc#Gf#",
        "#fG#cCDddDCc#Gf#",
        "#fG#cCDddDCc#Gf#",
        "#fG#cCCCCCCc#Gf#",
        "#fFg#cCCCCc#gFf#",
        "#fFgg#cccc#ggFf#",
        "#fFgGgGgGgGggFf#",
        "#fFFFFFFFFFFFFf#",
        "##ffffffffffff##",
        "################",
    ]
    save_variant("sound_trap_front", "variant_a_tactical_subwoofer", create_sprite(pal_front_a, pat_front_a))

    pal_side_a = {
        '#': '#020617', 'F': '#1e293b', 'f': '#334155', 'P': '#0f172a',
        'p': '#27272a', 'H': '#64748b', 'h': '#94a3b8',
    }
    pat_side_a = [
        "##ffffffffffff##",
        "#fFFFFFFFFFFFFf#",
        "#fFppppppppppFf#",
        "#fFp#HHHHHH#pFf#",
        "#fFp#HhhhhH#pFf#",
        "#fFp#H#PP#H#pFf#",
        "#fFp#HHHHHH#pFf#",
        "#fFppppppppppFf#",
        "#fFppppppppppFf#",
        "#fFppppppppppFf#",
        "#fFppppppppppFf#",
        "#fFppppppppppFf#",
        "#fFFFFFFFFFFFFf#",
        "##ffffffffffff##",
        "################",
        "################",
    ]
    save_variant("sound_trap_side", "variant_a_tactical_subwoofer", create_sprite(pal_side_a, pat_side_a))

    pal_top_a = {
        '#': '#020617', 'F': '#1e293b', 'f': '#334155', 'P': '#09090b',
        'D': '#64748b', 'd': '#cbd5e1', 'G': '#22c55e', 'Y': '#eab308', 'R': '#ef4444',
    }
    pat_top_a = [
        "##ffffffffffff##",
        "#fFFFFFFFFFFFFf#",
        "#fFPPPPPPPPPPFf#",
        "#fFP#Dd##Dd#PFf#",
        "#fFP#dD##dD#PFf#",
        "#fFPPPPPPPPPPFf#",
        "#fFP#GG#YY#RPFf#",
        "#fFP#GG#YY#RPFf#",
        "#fFPPPPPPPPPPFf#",
        "#fFP#Dd##Dd#PFf#",
        "#fFP#dD##dD#PFf#",
        "#fFPPPPPPPPPPFf#",
        "#fFFFFFFFFFFFFf#",
        "##ffffffffffff##",
        "################",
        "################",
    ]
    save_variant("sound_trap_top", "variant_a_tactical_subwoofer", create_sprite(pal_top_a, pat_top_a))

    # Variant B: Creeper Mimic Siren
    pal_front_b = {
        '#': '#052e16', 'C': '#15803d', 'c': '#22c55e', 'K': '#022c22',
        'k': '#000000', 'R': '#dc2626',
    }
    pat_front_b = [
        "################",
        "#cccccccccccccc#",
        "#cCCCCCCCCCCCCc#",
        "#cCC#kk##kk#CCc#", # Creeper eyes with red backlighting
        "#cCC#kR##Rk#CCc#",
        "#cCCCC#kk#CCCCc#", # Creeper nose & mouth
        "#cCCC#kkkk#CCCc#",
        "#cCC#kkkkkk#CCc#",
        "#cCC#kkkkkk#CCc#",
        "#cCC#k#kk#k#CCc#",
        "#cCCCCCCCCCCCCc#",
        "#cCCCCCCCCCCCCc#",
        "#cCCCCCCCCCCCCc#",
        "#cccccccccccccc#",
        "################",
        "################",
    ]
    save_variant("sound_trap_front", "variant_b_creeper_mimic", create_sprite(pal_front_b, pat_front_b))

    pal_side_b = {
        '#': '#052e16', 'C': '#15803d', 'c': '#22c55e', 'D': '#166534',
    }
    pat_side_b = [
        "################",
        "#cccccccccccccc#",
        "#cCDDDcCDDDcCDc#", # Acoustic pyramid foam
        "#cDDDcCDDDcCDDc#",
        "#cDDcCDDDcCDDDc#",
        "#cDcCDDDcCDDDDc#",
        "#cCDDDcCDDDcCDc#",
        "#cDDDcCDDDcCDDc#",
        "#cDDcCDDDcCDDDc#",
        "#cDcCDDDcCDDDDc#",
        "#cCDDDcCDDDcCDc#",
        "#cDDDcCDDDcCDDc#",
        "#cCCCCCCCCCCCCc#",
        "#cccccccccccccc#",
        "################",
        "################",
    ]
    save_variant("sound_trap_side", "variant_b_creeper_mimic", create_sprite(pal_side_b, pat_side_b))

    pal_top_b = {
        '#': '#1c1917', 'W': '#78350f', 'w': '#92400e', 'B': '#d97706',
        'b': '#f59e0b',
    }
    pat_top_b = [
        "################",
        "#wwwwwwwwwwwwww#",
        "#wWWWWWWWWWWWWw#",
        "#wWW##bbbb##WWw#", # Brass acoustic horn funnel
        "#wW#bbBBBBbb#Ww#",
        "#wW#bBB##BBb#Ww#",
        "#wW#bB#WW#Bb#Ww#",
        "#wW#bB#WW#Bb#Ww#",
        "#wW#bBB##BBb#Ww#",
        "#wW#bbBBBBbb#Ww#",
        "#wWW##bbbb##WWw#",
        "#wWWWWWWWWWWWWw#",
        "#wwwwwwwwwwwwww#",
        "################",
        "################",
        "################",
    ]
    save_variant("sound_trap_top", "variant_b_creeper_mimic", create_sprite(pal_top_b, pat_top_b))

    # Variant C: Cyber Sonic Projector
    pal_front_c = {
        '#': '#020617', 'C': '#0f172a', 'c': '#1e293b', 'L': '#06b6d4',
        'l': '#67e8f9', 'W': '#ffffff',
    }
    pat_front_c = [
        "################",
        "#cCCCCCCCCCCCCc#",
        "#C#ll######ll#C#",
        "#C#llLLLLLLll#C#",
        "#C#lLLCCCCLLl#C#",
        "#C#LC#llll#CL#C#",
        "#C#L#lWllWl#L#C#",
        "#C#L#llWWll#L#C#",
        "#C#L#lWllWl#L#C#",
        "#C#LC#llll#CL#C#",
        "#C#lLLCCCCLLl#C#",
        "#C#llLLLLLLll#C#",
        "#C#ll######ll#C#",
        "#cCCCCCCCCCCCCc#",
        "################",
        "################",
    ]
    save_variant("sound_trap_front", "variant_c_cyber_sonic", create_sprite(pal_front_c, pat_front_c))
    save_variant("sound_trap_side", "variant_c_cyber_sonic", create_sprite(pal_side_a, pat_side_a))
    save_variant("sound_trap_top", "variant_c_cyber_sonic", create_sprite(pal_front_c, pat_front_c))

# =========================================================================
# 11. LASER PYLON BLOCK
# =========================================================================
def gen_laser_pylon():
    print("Generating Laser Pylon variants...")

    # Variant A: Aegis Defense Pylon
    pal_base_a = {
        '#': '#020617', 'T': '#334155', 't': '#64748b', 'C': '#06b6d4',
        'c': '#22d3ee', 'B': '#94a3b8',
    }
    pat_base_a = [
        "################",
        "#tttttttttttttt#",
        "#tTTTTTTTTTTTTt#",
        "#tT#B#TTTT#B#Tt#",
        "#tTTTTTTTTTTTTt#",
        "#tTT#cccccc#TTt#",
        "#tTT#cCCCCc#TTt#",
        "#tTT#cCCCCc#TTt#",
        "#tTT#cCCCCc#TTt#",
        "#tTT#cccccc#TTt#",
        "#tTTTTTTTTTTTTt#",
        "#tT#B#TTTT#B#Tt#",
        "#tTTTTTTTTTTTTt#",
        "#tttttttttttttt#",
        "################",
        "################",
    ]
    save_variant("laser_pylon_base", "variant_a_aegis_defense", create_sprite(pal_base_a, pat_base_a))

    pal_post_a = {
        '#': '#020617', 'P': '#1e293b', 'p': '#334155', 'R': '#dc2626',
        'r': '#ef4444', 'w': '#fca5a5',
    }
    pat_post_a = [
        "....#pppp#......",
        "....#PppP#......",
        "...#rwrrr#......",
        "...#rRRRr#......",
        "....#PppP#......",
        "....#PppP#......",
        "...#rwrrr#......",
        "...#rRRRr#......",
        "....#PppP#......",
        "....#PppP#......",
        "...#rwrrr#......",
        "...#rRRRr#......",
        "....#PppP#......",
        "....#pppp#......",
        "................",
        "................",
    ]
    save_variant("laser_pylon_post", "variant_a_aegis_defense", create_sprite(pal_post_a, pat_post_a))

    pal_emitter_a = {
        '#': '#020617', 'G': '#334155', 'g': '#64748b', 'R': '#b91c1c',
        'r': '#ef4444', 'W': '#ffffff',
    }
    pat_emitter_a = [
        "......####......",
        "....#gggggg#....",
        "...#gGGGGGGg#...",
        "..#gG######Gg#..",
        "..#g#rrrrrr#g#..",
        ".#gG#rWWrrRr#Gg#",
        ".#gG#rWWrrrr#Gg#",
        ".#gG#rrrrrrR#Gg#",
        "..#g#rrrrrr#g#..",
        "..#gG######Gg#..",
        "...#gGGGGGGg#...",
        "....#gggggg#....",
        "......####......",
        "................",
        "................",
        "................",
    ]
    save_variant("laser_pylon_emitter", "variant_a_aegis_defense", create_sprite(pal_emitter_a, pat_emitter_a))

    # Variant B: Crying Obsidian / Netherite Pylon
    pal_base_b = {
        '#': '#1e1b4b', 'O': '#0f172a', 'P': '#7e22ce', 'p': '#a855f7',
        'N': '#1c1917',
    }
    pat_base_b = [
        "################",
        "#NNNNNNNNNNNNNN#",
        "#N#P#NNNNNN#P#N#",
        "#NNpNNNNNNNNpNN#",
        "#NNN#P#pp#P#NNN#",
        "#NNN#pppppp#NNN#",
        "#NNN#ppOOp#NNN#.",
        "#NNN#ppOOp#NNN#.",
        "#NNN#pppppp#NNN#",
        "#NNN#P#pp#P#NNN#",
        "#NNpNNNNNNNNpNN#",
        "#N#P#NNNNNN#P#N#",
        "#NNNNNNNNNNNNNN#",
        "################",
        "################",
        "################",
    ]
    save_variant("laser_pylon_base", "variant_b_crying_obsidian", create_sprite(pal_base_b, pat_base_b))

    pal_post_b = {
        '#': '#18181b', 'N': '#27272a', 'P': '#7e22ce', 'p': '#c084fc',
    }
    pat_post_b = [
        "....#NNNN#......",
        "....#NpNN#......",
        "...#ppppp#......",
        "...#pPPPP#......",
        "....#NpNN#......",
        "....#NNpN#......",
        "...#ppppp#......",
        "...#PPPPp#......",
        "....#NpNN#......",
        "....#NNpN#......",
        "...#ppppp#......",
        "...#pPPPP#......",
        "....#NNNN#......",
        "....#NNNN#......",
        "................",
        "................",
    ]
    save_variant("laser_pylon_post", "variant_b_crying_obsidian", create_sprite(pal_post_b, pat_post_b))

    pal_emitter_b = {
        '#': '#18181b', 'N': '#27272a', 'P': '#7e22ce', 'p': '#c084fc',
        'W': '#ffffff',
    }
    pat_emitter_b = [
        "......####......",
        "....#NNNNNN#....",
        "...#NNppppNN#...",
        "..#NN#PPPP#NN#..",
        "..#N#pppppp#N#..",
        ".#NN#pWWppPp#NN#",
        ".#NN#pWWpppp#NN#",
        ".#NN#ppppppP#NN#",
        "..#N#pppppp#N#..",
        "..#NN#PPPP#NN#..",
        "...#NNppppNN#...",
        "....#NNNNNN#....",
        "......####......",
        "................",
        "................",
        "................",
    ]
    save_variant("laser_pylon_emitter", "variant_b_crying_obsidian", create_sprite(pal_emitter_b, pat_emitter_b))

# =========================================================================
# 12. ERIK'S SWORD & STAR
# =========================================================================
def gen_erik_items():
    print("Generating Erik's Sword & Star variants...")

    # Sword Variant A: Shadow Katana
    pal_s_a = {
        '#': '#020617', 'O': '#0f172a', 'P': '#7e22ce', 'p': '#a855f7',
        'W': '#ffffff', 'G': '#eab308', 'H': '#1e293b',
    }
    pat_s_a = [
        "...............#",
        "..............#W",
        "............#pPO",
        "...........#pPO#",
        "..........#pPO#.",
        ".........#pPO#..",
        "........#pPO#...",
        ".......#pPO#....",
        "......#pPO#.....",
        ".....#pPO#......",
        "....#GG##.......",
        "...#HGG#........",
        "..#OH#..........",
        ".#HO#...........",
        "#G#.............",
        "................",
    ]
    save_variant("eriks_sword", "variant_a_shadow_katana", create_sprite(pal_s_a, pat_s_a))

    # Star Variant A: Razor Steel Shuriken
    pal_st_a = {
        '#': '#020617', 'S': '#64748b', 's': '#cbd5e1', 'W': '#ffffff',
        'P': '#7e22ce', 'p': '#c084fc',
    }
    pat_st_a = [
        ".......#W.......",
        "......#ss#......",
        "......#ss#......",
        ".....#sSSs#.....",
        "....#sSS#Ss#....",
        "...#sSS#Pp#Ss#..",
        "..#sS##pppp##Ss#",
        "#WssssppWWppssss",
        "..#sS##pppp##Ss#",
        "...#sSS#pP#Ss#..",
        "....#sSS#Ss#....",
        ".....#sSSs#.....",
        "......#ss#......",
        "......#ss#......",
        ".......#W.......",
        "................",
    ]
    save_variant("eriks_star", "variant_a_shadow_ninja", create_sprite(pal_st_a, pat_st_a))

    # Sword Variant B: Plasma Cyber Saber
    pal_s_b = {
        '#': '#030712', 'L': '#06b6d4', 'l': '#22d3ee', 'W': '#ffffff',
        'C': '#0f172a', 'c': '#1e293b',
    }
    pat_s_b = [
        "...............#",
        "..............#W",
        "............#lLL",
        "...........#lLL#",
        "..........#lLL#.",
        ".........#lLL#..",
        "........#lLL#...",
        ".......#lLL#....",
        "......#lLL#.....",
        ".....#lLL#......",
        "....#cc##.......",
        "...#Ccc#........",
        "..#cC#..........",
        ".#Cc#...........",
        "#c#.............",
        "................",
    ]
    save_variant("eriks_sword", "variant_b_plasma_energy", create_sprite(pal_s_b, pat_s_b))

    # Star Variant B: Plasma Throwing Glaive
    pal_st_b = {
        '#': '#030712', 'L': '#06b6d4', 'l': '#67e8f9', 'W': '#ffffff',
        'C': '#0f172a',
    }
    pat_st_b = [
        ".......#W.......",
        "......#ll#......",
        "......#ll#......",
        ".....#lLLl#.....",
        "....#lLL#Ll#....",
        "...#lLL#CC#Ll#..",
        "..#lL##CCCC##Ll#",
        "#WllllCCWWCCllll",
        "..#lL##CCCC##Ll#",
        "...#lLL#CC#Ll#..",
        "....#lLL#Ll#....",
        ".....#lLLl#.....",
        "......#ll#......",
        "......#ll#......",
        ".......#W.......",
        "................",
    ]
    save_variant("eriks_star", "variant_b_plasma_energy", create_sprite(pal_st_b, pat_st_b))

    # Sword Variant C: Royal Relic Broadsword
    pal_s_c = {
        '#': '#1c1917', 'S': '#94a3b8', 's': '#e2e8f0', 'G': '#eab308',
        'g': '#fde047', 'R': '#ef4444', 'L': '#78350f',
    }
    pat_s_c = [
        "...............#",
        "..............#s",
        "............#sSS",
        "...........#sSS#",
        "..........#sSS#.",
        ".........#sSS#..",
        "........#sSS#...",
        ".......#sSS#....",
        "......#sSS#.....",
        ".....#sSS#......",
        "....#Gg##.......",
        "...#LGG#........",
        "..#LL#..........",
        ".#LL#...........",
        "#R#.............",
        "................",
    ]
    save_variant("eriks_sword", "variant_c_royal_relic", create_sprite(pal_s_c, pat_s_c))

    # Star Variant C: Royal Gilded Star
    pal_st_c = {
        '#': '#1c1917', 'G': '#eab308', 'g': '#fde047', 'R': '#ef4444',
        'E': '#10b981', 'e': '#34d399',
    }
    pat_st_c = [
        ".......#R.......",
        "......#gg#......",
        "......#gg#......",
        ".....#gGGg#.....",
        "....#gGG#Gg#....",
        "...#gGG#Ee#Gg#..",
        "..#gG##eeee##Gg#",
        "#RggggEEeeEEgggg",
        "..#gG##eeee##Gg#",
        "...#gGG#Ee#Gg#..",
        "....#gGG#Gg#....",
        ".....#gGGg#.....",
        "......#gg#......",
        "......#gg#......",
        ".......#R.......",
        "................",
    ]
    save_variant("eriks_star", "variant_c_royal_relic", create_sprite(pal_st_c, pat_st_c))

def main():
    print("=== Generating SpyCraft High-Detail Asset Variants ===")
    gen_sniper_rifle()
    gen_recon_drone()
    gen_grappling_hook()
    gen_remote_detonator()
    gen_spy_tracker()
    gen_goggles()
    gen_laser_remote()
    gen_villager_disguise()
    gen_c4_block()
    gen_sound_trap()
    gen_laser_pylon()
    gen_erik_items()
    print("=== All asset variants generated successfully! ===")

if __name__ == "__main__":
    main()
