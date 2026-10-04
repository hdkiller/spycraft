# Minecraft Modding 101: Architecture & Concepts

This guide gives you the foundational mental model for Minecraft modding, specifically tailored for learning together with Erik.

---

## 1. Java Edition vs. Bedrock Edition

There are two major versions of Minecraft:

| Feature | Java Edition | Bedrock Edition |
| :--- | :--- | :--- |
| **Platforms** | PC, Mac, Linux | Windows 10/11, iOS/iPad, Android, Switch, Xbox, PS5 |
| **Language** | Java (Open source ecosystem) | C++ |
| **Modding** | **True Modding** (Bytecode injection, custom code, unlimited freedom) | Add-Ons / Marketplace (Limited scripting & behavior packs) |
| **Our Target** | **Java Edition (1.21.1)** | - |

> [!NOTE]
> Modding lives in **Java Edition**. If Erik plays on a Mac or PC, you can play your custom mods together directly. If Erik plays on an iPad or console, that is Bedrock edition — but Java Edition on your Mac is where all the programming magic happens!

---

## 2. The Modding Ecosystem: Fabric vs. Forge / NeoForge

To load custom code into Minecraft, the game needs a **Mod Loader**. The two main mod loaders are:

1. **Fabric (Our Choice)**:
   - Modern, modular, and extremely fast.
   - Compiles in seconds rather than minutes.
   - Clean, lightweight APIs with minimal bloat.
   - The developer favorite for modern versions (1.14 through 1.21+).
2. **NeoForge / Forge**:
   - The historic heavy-weight loader. Has lots of legacy modpacks, but is slower to compile and heavier for beginners.

**We configured Fabric for version 1.21.1** (the latest rock-solid LTS modding release).

---

## 3. How Minecraft Code Works: Deobfuscation & Mappings

Mojang (the creators of Minecraft) ships the game with **obfuscated code** — meaning variable names, class names, and methods are scrambled into random letters (like `class a`, method `b()`) to reduce file size.

### Loom & Mojang Official Mappings
When you run `./gradlew build` or `./gradlew runClient`:
1. **Fabric Loom** (our Gradle plugin) automatically downloads the official Minecraft jar from Mojang.
2. It applies **Mojang's official deobfuscation mappings**.
3. Now, in your code editor, you write readable Java:
   ```java
   player.sendSystemMessage(Component.literal("Hello Erik!"));
   level.addFreshEntity(lightning);
   ```
4. When you build the mod (`./gradlew build`), Loom re-maps your code back into the format the game engine understands and outputs a `.jar` file.

---

## 4. Mod Structure & Lifecycle

In Fabric, every mod has three key parts:

### A. Mod Initializer (`SpyCraftMod.java`)
Implements `ModInitializer`. When Minecraft boots up, Fabric calls `onInitialize()`:
```java
public class SpyCraftMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // Register items, blocks, sounds, and events here!
    }
}
```

### B. Registries (`BuiltInRegistries`)
Minecraft stores everything in **Registries** (Items, Blocks, Entities, Biomes, Sounds). To add something new, you simply register it with your mod's namespace:
```java
Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("spycraft", "my_item"), myItem);
```

### C. Assets & Resource Packs (`src/main/resources/assets/spycraft/`)
Minecraft separates code from visuals:
- `lang/en_us.json` & `lang/hu_hu.json`: Translations displayed in the game UI.
- `models/item/*.json`: Tells Minecraft how to display the 3D or 2D item model.
- `textures/item/*.png`: 16x16 pixel art textures.
