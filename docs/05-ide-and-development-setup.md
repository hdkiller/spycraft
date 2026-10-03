# 🛠️ IDE & Development Setup Guide (VS Code / Antigravity IDE)

This guide covers setting up and developing **SpyCraft** in **Visual Studio Code** or **Antigravity IDE** on macOS.

---

## 1. Prerequisites

- **Java 21 (LTS)**:
  Ensure OpenJDK 21 is installed via Homebrew:
  ```bash
  brew install openjdk@21
  ```
  The workspace is preconfigured to use the stable Homebrew symlink:
  `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`

- **Recommended VS Code Extensions**:
  When opening the workspace, VS Code will prompt you to install recommended extensions (defined in [`.vscode/extensions.json`](../.vscode/extensions.json)):
  - **Extension Pack for Java** (`vscjava.vscode-java-pack`)
  - **Gradle for Java** (`vscjava.vscode-gradle`)
  - **Language Support for Java™ by Red Hat** (`redhat.java`)

---

## 2. Opening the Workspace

You can open the workspace in two ways:

1. **Open Root Folder**: Open `~/Develop/mc` directly in VS Code / Antigravity IDE.
   The root `.vscode/` directory contains all tasks and launch profiles ready to go.
2. **Open Multi-Root Workspace**: Open [`mc.code-workspace`](../mc.code-workspace) via `File` $\rightarrow$ `Open Workspace from File...`.

---

## 3. Running & Debugging (F5)

To start the Minecraft Client with the full Java debugger attached:

1. Press **F5** (or open the **Run & Debug** panel on the left and select **Minecraft Client (Debug)**).
2. Fabric Loom will launch Minecraft 1.21.1 with SpyCraft loaded.
3. You can set breakpoints directly in your Java code (e.g. in [`ReconDroneManager.java`](../spycraft/src/main/java/com/hdkiller/spycraft/drone/ReconDroneManager.java) or [`SniperRifleItem.java`](../spycraft/src/main/java/com/hdkiller/spycraft/item/SniperRifleItem.java)) to inspect live variables, entity state, and raycasts!

---

## 4. Built-in Tasks (Command Palette)

Press `Cmd+Shift+P` (or `Ctrl+Shift+P`), select **Tasks: Run Task**, and choose:

- **Run Minecraft Client**: Runs `./gradlew runClient` in the integrated terminal.
- **Build SpyCraft Mod (JAR)**: Runs `./gradlew build` to compile `spycraft/build/libs/spycraft-1.0.0.jar`.
- **Run Minecraft Dedicated Server**: Runs `./gradlew runServer` to test multiplayer compatibility.
- **Generate Loom IDE Runs**: Runs `./gradlew vscode` to refresh Loom launch configurations if Gradle dependencies change.

---

## 5. Troubleshooting

- **"Java runtime not found"**:
  Check `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`. If using a different Java 21 distribution, update `.vscode/settings.json`.
- **"Loom configuration cache invalid"**:
  Run `./gradlew --no-configuration-cache build` in `spycraft/` to force a clean build cache.
- **"Classpath not recognizing Minecraft classes"**:
  In VS Code, press `Cmd+Shift+P` $\rightarrow$ `Java: Clean Java Language Server Workspace` $\rightarrow$ `Restart and delete`.
