#!/usr/bin/env python3
"""
SpyCraft Asset Harness Local Server
Provides a live web interface to preview, compare, inspect in 3D, and switch asset variants.
"""

import os
import sys
import json
import shutil
import hashlib
from http.server import HTTPServer, SimpleHTTPRequestHandler
import urllib.parse

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
VARIANTS_DIR = os.path.join(BASE_DIR, "variants")
MOD_TEX_DIR = os.path.join(BASE_DIR, "../../src/main/resources/assets/spycraft/textures")

from cli_apply import ASSET_MAP, THEME_PRESETS, apply_variant, apply_preset

PORT = 8088

METADATA = {
    "sniper_rifle": {
        "title": "Sniper Rifle",
        "category": "weapons",
        "description": "Long-range precision rifle with progressive 3-stage optical zoom scope and tracer visual effects.",
        "variants": {
            "current": {"name": "Baseline Stick", "desc": "Original minimal diagonal prototype."},
            "variant_a_barrett50": {"name": "SpecOps Barrett .50", "desc": "Heavy anti-materiel rifle, fluted barrel, muzzle brake, green optical scope & bipod.", "theme": "specops"},
            "variant_b_covert_dmr": {"name": "Ghost Suppressed DMR", "desc": "Matte black tactical DMR with integral barrel suppressor & holographic red-dot optic.", "theme": "specops"},
            "variant_c_plasma_railgun": {"name": "Cyber Plasma Railgun", "desc": "Titanium chassis with twin magnetic accelerator rails & glowing cyan plasma coils.", "theme": "cyberpunk"},
            "variant_d_timberline_hunter": {"name": "Timberline Classic Sniper", "desc": "Polished walnut stock, blued steel receiver, brass scope rings & swivels.", "theme": "steampunk"},
        }
    },
    "recon_drone": {
        "title": "Recon Drone",
        "category": "surveillance",
        "description": "Controllable surveillance quadcopter with 60s battery, 360 gimbal camera, thermal scanner & sleep dart launcher.",
        "variants": {
            "current": {"name": "Baseline Quad", "desc": "Original 4-dot prototype sprite."},
            "variant_a_phantom_quad": {"name": "Shadow Phantom Quad", "desc": "Carbon composite X-frame, 4 spinning rotor discs, central gimbal lens & red beacon.", "theme": "specops"},
            "variant_b_stealth_predator": {"name": "Viper Stealth UAV", "desc": "Delta-wing stealth UAV with amber forward sensor array and dual rear jet exhausts.", "theme": "specops"},
            "variant_c_cyber_orb": {"name": "Aegis Recon Sphere", "desc": "Floating anti-gravity orb with cyan levitation ring and giant blue cyclops lens.", "theme": "cyberpunk"},
            "variant_d_redstone_scout": {"name": "Clockwork Redstone Flyer", "desc": "Brass gear casing, copper rotor arms, glowing redstone dust core.", "theme": "steampunk"},
        }
    },
    "grappling_hook_gun": {
        "title": "Grappling Hook Gun",
        "category": "weapons",
        "description": "Tactical climbing tool firing a barbed cable anchor that pulls the operative swiftly up vertical cliffs.",
        "variants": {
            "current": {"name": "Baseline Hook", "desc": "Original minimal sprite."},
            "variant_a_tactical_winch": {"name": "Pneumatic Line Gun", "desc": "Tactical pistol with 3-prong steel grapple, braided wire spool & silver CO2 cylinder.", "theme": "specops"},
            "variant_b_crossbow_grapple": {"name": "Cross-Tether Launcher", "desc": "Folding recurve crossbow limbs, high-tension wire string & heavy barbed penetrator bolt.", "theme": "specops"},
            "variant_c_plasma_harpoon": {"name": "Mag-Tether Harpoon", "desc": "Sci-fi magnetic launcher with twin cyan accelerator rails & hazard stripes.", "theme": "cyberpunk"},
            "variant_d_steampunk_grappler": {"name": "Steampunk Brass Winch", "desc": "Burnished copper & brass frame, forged iron anchor hook & gear ratchet.", "theme": "steampunk"},
        }
    },
    "remote_detonator": {
        "title": "Remote Detonator",
        "category": "explosives",
        "description": "Multi-stage radio trigger for placed C4 plastic explosives with 3-level yield cycling (1x / 2x / 4x power).",
        "variants": {
            "current": {"name": "Baseline Detonator", "desc": "Original grey box prototype."},
            "variant_a_military_clamshell": {"name": "Pelican Rugged Clamshell", "desc": "Olive-drab ruggedized housing, flip-up red safety switch, green LED frequency & antenna.", "theme": "specops"},
            "variant_b_cyber_datapad": {"name": "Tactical Glass Datapad", "desc": "Bezel-less datapad with cyan touchscreen 3-bar yield meter and glowing detonate zone.", "theme": "cyberpunk"},
            "variant_c_hazard_plunger": {"name": "Industrial Blast Trigger", "desc": "Yellow/black hazard striping, heavy red plunger button & rotary yield selector.", "theme": "steampunk"},
            "variant_d_redstone_transmitter": {"name": "Arcane Redstone Activator", "desc": "Polished deepslate casing, glowing redstone torch crystal emitter & gold key switch.", "theme": "steampunk"},
        }
    },
    "spy_tracker": {
        "title": "Spy Tracker (Radar)",
        "category": "surveillance",
        "description": "Handheld field radar tracking nearby living entities with compass direction heading, distance, and elevation readout.",
        "variants": {
            "current": {"name": "Baseline Radar", "desc": "Original blue/green square prototype."},
            "variant_a_crt_radar_pda": {"name": "Tactical CRT Radar", "desc": "Rugged slate PDA with phosphor-green circular CRT radar screen, sweep beam & hostile blips.", "theme": "specops"},
            "variant_b_holo_compass": {"name": "Holographic Waypoint Scanner", "desc": "Round titanium housing with floating holographic cyan directional navigation chevron.", "theme": "cyberpunk"},
            "variant_c_amber_sonar": {"name": "Amber Sonar Gauge", "desc": "Submarine brass/steel dial with glowing amber-gold sonar scope & needle scanner.", "theme": "steampunk"},
            "variant_d_tactical_tablet": {"name": "Military Topo Tablet", "desc": "Widescreen military slate showing green topographic map contour lines & GPS beacon.", "theme": "specops"},
        }
    },
    "spy_goggles": {
        "title": "Night Vision Spy Goggles",
        "category": "surveillance",
        "description": "Tactical headgear granting operative night vision and enemy thermal highlighting.",
        "variants": {
            "current": {"name": "Baseline Goggles", "desc": "Original minimal wireframe sprite."},
            "variant_a_pvs31_dual_nvg": {"name": "PVS-31 Dual NVG", "desc": "Twin cylindrical optical barrels with glowing emerald night vision lenses & brow mount.", "theme": "specops"},
            "variant_b_cyber_hud_visor": {"name": "Cyber Neon HUD Visor", "desc": "Aerodynamic wrap-around carbon visor with glowing green heads-up display scanlines.", "theme": "cyberpunk"},
            "variant_c_gpnvg_quad_panoramic": {"name": "GPNVG-18 Panoramic NVG", "desc": "Four panoramic green night-vision tubes with heavy helmet shroud mounting.", "theme": "specops"},
            "variant_d_steampunk_aviators": {"name": "Brass Clockwork Aviators", "desc": "Brown leather-padded eye cups with riveted brass bezels & emerald crystal lenses.", "theme": "steampunk"},
        }
    },
    "gps_navigator_goggles": {
        "title": "GPS Navigator Goggles",
        "category": "surveillance",
        "description": "Advanced HUD eyewear overlaying waypoint navigation markers directly in the operative's field of view.",
        "variants": {
            "current": {"name": "Baseline GPS", "desc": "Original blue frame sprite."},
            "variant_a_pvs31_dual_nvg": {"name": "PVS-31 Cyan HUD", "desc": "Twin tactical optic tubes with holographic cyan GPS navigation lenses.", "theme": "specops"},
            "variant_b_cyber_hud_visor": {"name": "Cyber Cyan Visor", "desc": "Streamlined neon cyan heads-up display visor with digital reticle markings.", "theme": "cyberpunk"},
            "variant_c_gpnvg_quad_panoramic": {"name": "Panoramic Cyan Nav", "desc": "Wide 4-lens panoramic HUD headgear with blue optical glass.", "theme": "specops"},
            "variant_d_steampunk_aviators": {"name": "Clockwork Sapphire Goggles", "desc": "Brass aviator eyecups fitted with glowing blue sapphire lenses.", "theme": "steampunk"},
        }
    },
    "laser_remote": {
        "title": "Laser Defense Remote",
        "category": "weapons",
        "description": "Command transmitter pairing, calibrating, and synchronizing Laser Defense Pylons into a lethal perimeter barrier.",
        "variants": {
            "current": {"name": "Baseline Remote", "desc": "Original simple transmitter sprite."},
            "variant_a_laser_designator": {"name": "Tactical Laser Designator", "desc": "Matte black pistol remote with red laser diode aperture & status indicators.", "theme": "specops"},
            "variant_b_security_keycard": {"name": "Security Access Keycard", "desc": "Sleek clearance keycard with gold microchip & holographic laser grid.", "theme": "cyberpunk"},
            "variant_c_field_transmitter": {"name": "Hazard Command Transmitter", "desc": "Industrial yellow-striped housing with heavy rubber toggle & red beacon.", "theme": "steampunk"},
            "variant_d_cyber_remote": {"name": "Cyber Optical Remote", "desc": "Futuristic titanium controller with cyan targeting laser emitter.", "theme": "cyberpunk"},
        }
    },
    "villager_disguise_mask": {
        "title": "Villager Disguise Mask",
        "category": "disguise",
        "description": "Infiltration facial disguise pacifying Iron Golems and concealing operative identity.",
        "variants": {
            "current": {"name": "Baseline Mask", "desc": "Original flat face prototype."},
            "variant_a_carved_wooden_mask": {"name": "Carved Birch Mask", "desc": "Hand-carved birch wood grain, iconic unibrow, 3D nose & cut-out spy eyeholes.", "theme": "specops"},
            "variant_b_burlap_stitched_mask": {"name": "Stitched Burlap Face", "desc": "Rough wool/burlap texture with dark stitched seams & hidden mesh eye slits.", "theme": "specops"},
            "variant_c_porcelain_theatrical": {"name": "Porcelain Commedia Mask", "desc": "Ivory porcelain theatrical mask with stylized emerald accents & refined nose.", "theme": "cyberpunk"},
        }
    },
    "villager_disguise_robe": {
        "title": "Villager Disguise Robe",
        "category": "disguise",
        "description": "Chest armor disguise completing the operative's civilian cover identity.",
        "variants": {
            "current": {"name": "Baseline Robe", "desc": "Original brown cloak sprite."},
            "variant_a_emerald_merchant_robe": {"name": "Emerald Merchant Garb", "desc": "Rich terracotta villager cloth with cowl hood & gold-set emerald brooch.", "theme": "steampunk"},
            "variant_b_tactical_ghillie_cloak": {"name": "Tactical Ghillie Mantle", "desc": "Foliage camouflage pattern with deep cowl shadow & chest utility straps.", "theme": "specops"},
            "variant_c_purple_cleric_robe": {"name": "Ceremonial Cleric Robe", "desc": "Deep purple cloth with gold and lapis ceremonial embroidery & cowl.", "theme": "cyberpunk"},
        }
    },
    "c4_side": {
        "title": "C4 Explosive (Side Face)",
        "category": "explosives",
        "description": "Side texture of the remote-triggered plastic explosive block.",
        "variants": {
            "current": {"name": "Baseline C4 Side", "desc": "Original grey explosive block face."},
            "variant_a_military_c4": {"name": "Military C4 Bricks", "desc": "4 plastic explosive bricks in OD duct tape, digital LCD timer & red/blue detonator wires.", "theme": "specops"},
            "variant_b_hazard_mining": {"name": "Hazard Mining Blast Side", "desc": "Reinforced steel framing, yellow/black hazard chevrons & armed red charge core.", "theme": "steampunk"},
            "variant_c_cyber_nanite": {"name": "Cyber Nanite Demolition", "desc": "Carbon-fiber block with pulsing cyan plasma core behind protective metal grating.", "theme": "cyberpunk"},
        }
    },
    "c4_top": {
        "title": "C4 Explosive (Top Face)",
        "category": "explosives",
        "description": "Top face of the remote C4 block showing receiver antenna socket and blasting cap terminals.",
        "variants": {
            "current": {"name": "Baseline C4 Top", "desc": "Original top sprite."},
            "variant_a_military_c4": {"name": "Military C4 Top", "desc": "Crossed olive duct tape bindings, blasting cap pass-through & central antenna socket.", "theme": "specops"},
            "variant_b_hazard_mining": {"name": "Hazard Hatch Top", "desc": "Bolted industrial access hatch with hazard warning stencil & detonator well.", "theme": "steampunk"},
            "variant_c_cyber_nanite": {"name": "Cyber Nanite Top", "desc": "Hexagonal titanium lock ring with cyan glowing conduit traces.", "theme": "cyberpunk"},
        }
    },
    "sound_trap_front": {
        "title": "Sound Trap (Speaker Front)",
        "category": "explosives",
        "description": "Front face of the acoustic sound trap emitting creature lure frequencies.",
        "variants": {
            "current": {"name": "Baseline Front", "desc": "Original dark face prototype."},
            "variant_a_tactical_subwoofer": {"name": "Tactical Subwoofer Lure", "desc": "Heavy steel speaker grille with visible massive subwoofer cone, dust cap & red lure LED.", "theme": "specops"},
            "variant_b_creeper_mimic": {"name": "Creeper Mimic Siren", "desc": "Dark green acoustic mesh embossed with the iconic Creeper face and glowing red backlighting.", "theme": "steampunk"},
            "variant_c_cyber_sonic": {"name": "Cyber Sonic Projector", "desc": "Concentric acoustic projector rings emitting holographic cyan sonic soundwaves.", "theme": "cyberpunk"},
        }
    },
    "sound_trap_side": {
        "title": "Sound Trap (Flight Case Side)",
        "category": "explosives",
        "description": "Side face of the sound decoy block.",
        "variants": {
            "current": {"name": "Baseline Side", "desc": "Original side sprite."},
            "variant_a_tactical_subwoofer": {"name": "Reinforced Flight Case", "desc": "Rugged steel ball corners, spring-loaded recessed carrying handle plate.", "theme": "specops"},
            "variant_b_creeper_mimic": {"name": "Acoustic Foam Panels", "desc": "Sound-absorbing camouflage green acoustic pyramid foam tiles.", "theme": "steampunk"},
            "variant_c_cyber_sonic": {"name": "Brushed Alloy Chassis", "desc": "Brushed titanium casing with recessed handles.", "theme": "cyberpunk"},
        }
    },
    "sound_trap_top": {
        "title": "Sound Trap (Control Panel Top)",
        "category": "explosives",
        "description": "Top control interface of the sound trap with tuning dials and equalizer meters.",
        "variants": {
            "current": {"name": "Baseline Top", "desc": "Original top sprite."},
            "variant_a_tactical_subwoofer": {"name": "Audio Mixing Console", "desc": "Volume & pitch rotary knobs with illuminated 5-band LED equalizer meter.", "theme": "specops"},
            "variant_b_creeper_mimic": {"name": "Brass Siren Horn", "desc": "Polished brass acoustic horn manifold and valve switches.", "theme": "steampunk"},
            "variant_c_cyber_sonic": {"name": "Hologram Touch Surface", "desc": "Circular holographic frequency visualizer touchpad.", "theme": "cyberpunk"},
        }
    },
    "laser_pylon_base": {
        "title": "Laser Pylon Base",
        "category": "explosives",
        "description": "Foundation block of the perimeter laser pylon.",
        "variants": {
            "current": {"name": "Baseline Base", "desc": "Original flat base sprite."},
            "variant_a_aegis_defense": {"name": "Tungsten Anchor Base", "desc": "Heavy bolted tungsten baseplate with triangular stability feet & cyan conduit ring.", "theme": "specops"},
            "variant_b_crying_obsidian": {"name": "Obsidian Runic Base", "desc": "Carved crying obsidian with glowing purple rune inlays.", "theme": "cyberpunk"},
        }
    },
    "laser_pylon_post": {
        "title": "Laser Pylon Post",
        "category": "explosives",
        "description": "Vertical energy column channeling high-voltage laser pulses.",
        "variants": {
            "current": {"name": "Baseline Post", "desc": "Original column sprite."},
            "variant_a_aegis_defense": {"name": "Armored Redstone Post", "desc": "Vertical conduit post wrapped in glowing redstone focus coil rings.", "theme": "specops"},
            "variant_b_crying_obsidian": {"name": "Netherite Vein Post", "desc": "Netherite alloy column with spiraling purple energy veins.", "theme": "cyberpunk"},
        }
    },
    "laser_pylon_emitter": {
        "title": "Laser Pylon Emitter",
        "category": "explosives",
        "description": "Focusing lens crowning the laser pylon.",
        "variants": {
            "current": {"name": "Baseline Emitter", "desc": "Original emitter sprite."},
            "variant_a_aegis_defense": {"name": "Gimbaled Ruby Crystal", "desc": "Precision faceted ruby focusing prism inside an armored gimbal ring.", "theme": "specops"},
            "variant_b_crying_obsidian": {"name": "Floating Ender Prism", "desc": "Crystalline lens emitting radiant beam focus.", "theme": "cyberpunk"},
        }
    },
    "eriks_sword": {
        "title": "Erik's Sword",
        "category": "weapons",
        "description": "Legendary blade delivering devastating bonus damage against hostile operatives.",
        "variants": {
            "current": {"name": "Baseline Sword", "desc": "Original minimal prototype."},
            "variant_a_shadow_katana": {"name": "Shadow Obsidian Katana", "desc": "Curved obsidian blade with glowing purple hamon edge, gold tsuba guard & wrap hilt.", "theme": "specops"},
            "variant_b_plasma_energy": {"name": "Plasma Energy Saber", "desc": "Vibrant cyan laser blade with emitter vents and dark carbon grip.", "theme": "cyberpunk"},
            "variant_c_royal_relic": {"name": "Royal Relic Broadsword", "desc": "Damascus patterned steel blade with gold crossguard and ruby pommel.", "theme": "steampunk"},
        }
    },
    "eriks_star": {
        "title": "Erik's Star",
        "category": "weapons",
        "description": "Deadly aerodynamic ninja shuriken with ricochet capabilities.",
        "variants": {
            "current": {"name": "Baseline Star", "desc": "Original simple star prototype."},
            "variant_a_shadow_ninja": {"name": "Shadow Razor Shuriken", "desc": "4-point razor steel shuriken with beveled edges and purple central gem.", "theme": "specops"},
            "variant_b_plasma_energy": {"name": "Plasma Throwing Glaive", "desc": "High-velocity throwing star glowing with cyan plasma energy blades.", "theme": "cyberpunk"},
            "variant_c_royal_relic": {"name": "Royal Gilded Star", "desc": "Gilded ceremonial throwing star with emerald center & ruby tips.", "theme": "steampunk"},
        }
    },
    "mission_beacon": {
        "title": "Mission Beacon (Küldetés Jeladó)",
        "category": "surveillance",
        "description": "Orbital beacon deployer summoning the 96-story Spy Base Skyscraper with guard units and laser defenses.",
        "variants": {
            "current": {"name": "Cyber Uplink Beacon", "desc": "Dark slate frame with cyber-cyan crystal core and red telemetry antenna.", "theme": "cyberpunk"}
        }
    },
    "thermal_goggles": {
        "title": "Thermal Vision Goggles (Hőkamera)",
        "category": "surveillance",
        "description": "Tactical headgear with thermal heat spectrum lenses detecting entities through solid walls.",
        "variants": {
            "current": {"name": "FLIR Thermal Visor", "desc": "Reinforced brow frame with red-to-white heat-signature spectrum lens array.", "theme": "specops"}
        }
    },
    "hologram_projector": {
        "title": "Hologram Decoy Projector",
        "category": "surveillance",
        "description": "Deployable cyber puck emitting a taunting holographic decoy that draws hostile mob aggro.",
        "variants": {
            "current": {"name": "Cyber Holo Puck", "desc": "Dark slate puck with radiating cyan holographic emitter rings.", "theme": "cyberpunk"}
        }
    },
    "tranquilizer_gun": {
        "title": "Tranquilizer Dart Gun (Altató Nyílpuska)",
        "category": "weapons",
        "description": "Silent dart pistol firing neurotoxin sleep darts that immobilize and disarm hostiles.",
        "variants": {
            "current": {"name": "SpecOps Tranq Pistol", "desc": "Matte black tactical chassis with luminous green sleep-toxin syringe vial.", "theme": "specops"}
        }
    },
    "smoke_grenade": {
        "title": "Tactical Smoke Grenade (Füstgránát)",
        "category": "weapons",
        "description": "Deployable canister generating an 11-meter dense smoke cloud for stealth infiltration and escape.",
        "variants": {
            "current": {"name": "Mil-Spec Smoke Canister", "desc": "Tactical slate cylinder with white designation band and steel pull ring.", "theme": "specops"}
        }
    },
    "climbing_gloves": {
        "title": "Magnetic Climbing Gloves (Mászókesztyű)",
        "category": "weapons",
        "description": "Reinforced tactical gloves with electromagnetic micro-nodes allowing vertical wall-scaling and suspended clinging.",
        "variants": {
            "current": {"name": "Cyber Mag-Grip Gloves", "desc": "Reinforced fingerless gloves with pulsing cyan magnetic node tips.", "theme": "cyberpunk"}
        }
    },
    "parachute_backpack": {
        "title": "Tactical Parachute Backpack (Ejtőernyő)",
        "category": "surveillance",
        "description": "Wearable tactical parachute rig: auto-deploys upon falling or can be emergency activated directly from hand.",
        "variants": {
            "current": {"name": "SpecOps Ripstop Rig", "desc": "Reinforced military harness pack with red ripcord release and high-visibility warning tag.", "theme": "specops"}
        }
    }
}

def file_sha256(path):
    if not os.path.exists(path):
        return None
    with open(path, "rb") as f:
        return hashlib.sha256(f.read()).hexdigest()

class HarnessHandler(SimpleHTTPRequestHandler):
    def translate_path(self, path):
        parsed = urllib.parse.urlparse(path).path
        if parsed.startswith("/variants/"):
            rel = parsed[len("/variants/"):]
            return os.path.join(VARIANTS_DIR, rel)
        elif parsed == "/spycraft-1.0.0.jar":
            jar_path = os.path.join(BASE_DIR, "../../build/libs/spycraft-1.0.0.jar")
            if os.path.exists(jar_path):
                return jar_path
            return os.path.join(BASE_DIR, "spycraft-1.0.0.jar")
        elif parsed == "/" or parsed == "/index.html":
            return os.path.join(BASE_DIR, "index.html")
        return os.path.join(BASE_DIR, parsed.lstrip("/"))

    def do_GET(self):
        if self.path == "/api/inventory" or self.path.startswith("/api/inventory?"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            
            catalog = []
            for asset, (rel_path, kind) in sorted(ASSET_MAP.items()):
                vdir = os.path.join(VARIANTS_DIR, asset)
                active_file = os.path.join(MOD_TEX_DIR, rel_path)
                active_hash = file_sha256(active_file)
                
                meta = METADATA.get(asset, {
                    "title": asset.replace("_", " ").title(),
                    "category": "other",
                    "description": "",
                    "variants": {}
                })
                
                v_list = []
                if os.path.exists(vdir):
                    for vf in sorted(os.listdir(vdir)):
                        if not vf.endswith(".png"): continue
                        vkey = os.path.splitext(vf)[0]
                        vpath = os.path.join(vdir, vf)
                        vhash = file_sha256(vpath)
                        is_active = (vhash == active_hash)
                        
                        vmeta = meta.get("variants", {}).get(vkey, {})
                        v_list.append({
                            "key": vkey,
                            "filename": vf,
                            "name": vmeta.get("name", vkey.replace("_", " ").title()),
                            "desc": vmeta.get("desc", ""),
                            "theme": vmeta.get("theme", "custom"),
                            "is_active": is_active,
                            "url": f"/variants/{asset}/{vf}"
                        })
                
                catalog.append({
                    "id": asset,
                    "title": meta.get("title", asset.replace("_", " ").title()),
                    "category": meta.get("category", "other"),
                    "kind": kind,
                    "mod_path": rel_path,
                    "description": meta.get("description", ""),
                    "variants": v_list
                })
                
            res = {
                "assets": catalog,
                "presets": THEME_PRESETS
            }
            self.wfile.write(json.dumps(res, indent=2).encode("utf-8"))
            return
            
        return super().do_GET()

    def do_POST(self):
        if self.path == "/api/apply":
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length).decode("utf-8")
            data = json.loads(body)
            asset = data.get("asset")
            variant = data.get("variant")
            
            success = apply_variant(asset, variant)
            self.send_response(200 if success else 400)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps({"success": success, "asset": asset, "variant": variant}).encode("utf-8"))
            return

        elif self.path == "/api/apply-preset":
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length).decode("utf-8")
            data = json.loads(body)
            preset = data.get("preset")
            
            success = apply_preset(preset)
            self.send_response(200 if success else 400)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps({"success": success, "preset": preset}).encode("utf-8"))
            return

        self.send_response(404)
        self.end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

def run_server(port=PORT):
    server = HTTPServer(("0.0.0.0", port), HarnessHandler)
    print(f"============================================================")
    print(f"  SpyCraft Asset Studio & Live Harness Running!")
    print(f"  Open in Browser: http://0.0.0.0:{port} or http://127.0.0.1:{port}")
    print(f"============================================================")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping harness server.")
        server.server_close()

if __name__ == "__main__":
    p = PORT
    if len(sys.argv) > 1 and sys.argv[1].isdigit():
        p = int(sys.argv[1])
    run_server(p)
