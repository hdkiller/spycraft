# Making Textures & 3D Models with Erik

One of the most rewarding parts of Minecraft modding with kids is seeing their own art appear inside the 3D world!

---

## 1. Pixel Art Basics (16x16)

Vanilla Minecraft items are **16x16 pixels**.

### Recommended Tools
1. **Blockbench (Highly Recommended)**:
   - Free, open-source 3D model editor & pixel art painter built specifically for Minecraft.
   - Install via Homebrew on Mac:
     ```bash
     brew install --cask blockbench
     ```
   - Erik can draw 2D pixel art textures or build entire 3D custom models (swords, shields, helmets, custom mobs/bosses).
2. **Online Pixel Art Editors (No install required)**:
   - **Piskel** ([piskelapp.com](https://www.piskelapp.com)): Great for beginners, can draw with grid and export PNG.
   - **Lospec Pixel Editor** ([lospec.com/pixel-editor](https://lospec.com/pixel-editor)): Simple palette-focused pixel painter.

### Texture Guidelines
- Save as transparent PNG (RGBA).
- Dimensions must be power of two (typically **16x16**, but **32x32** or **64x64** HD textures work too!).
- Place files in:
  `src/main/resources/assets/erikcraft/textures/item/<item_id>.png`

---

## 2. Item Models (`models/item/*.json`)

Minecraft maps the 2D PNG into a handheld 3D object using simple JSON files:

### For Standard Items (like Erik's Star):
`src/main/resources/assets/erikcraft/models/item/eriks_star.json`
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "erikcraft:item/eriks_star"
  }
}
```

### For Weapons / Tools (held like a sword):
`src/main/resources/assets/erikcraft/models/item/eriks_sword.json`
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "erikcraft:item/eriks_sword"
  }
}
```
*(Notice `"parent": "minecraft:item/handheld"` — this automatically makes the player hold it like a weapon at a diagonal angle!)*
