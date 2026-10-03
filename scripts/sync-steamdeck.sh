#!/usr/bin/env bash
# SpyCraft Steam Deck Sync Tool
# Builds the latest mod jar and copies it to Prism Launcher on Steam Deck over SSH.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SPYCRAFT_DIR="$REPO_ROOT/spycraft"
BUILD_JAR="$SPYCRAFT_DIR/build/libs/spycraft-1.0.0.jar"
DECK_HOST="${STEAMDECK_HOST:-steamdeck}"
DECK_INSTANCES_DIR="/home/deck/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances"

echo "============================================================"
echo " 🎮 SpyCraft -> Steam Deck Deployment"
echo "============================================================"

# 1. Check if Steam Deck is reachable
echo "[1/4] Checking SSH connection to Steam Deck ($DECK_HOST)..."
if ! ssh -o ConnectTimeout=3 -o BatchMode=yes "$DECK_HOST" "uname -n" >/dev/null 2>&1; then
    echo "❌ Error: Cannot connect to Steam Deck ($DECK_HOST) over SSH."
    echo "   Ensure the Steam Deck is powered on and connected to your Wi-Fi network."
    echo "   (Current IP in ~/.ssh/config: $(grep -A 2 'Host steamdeck' ~/.ssh/config | grep HostName | awk '{print $2}'))"
    exit 1
fi
echo "  ✓ Connected to Steam Deck successfully!"

# 2. Build the latest JAR if requested or if missing
SKIP_BUILD="${1:-}"
if [[ "$SKIP_BUILD" != "--skip-build" ]]; then
    echo "[2/4] Building latest SpyCraft JAR..."
    cd "$SPYCRAFT_DIR"
    ./gradlew build -x test
    cd "$REPO_ROOT"
else
    echo "[2/4] Skipping build step (--skip-build specified)."
fi

if [[ ! -f "$BUILD_JAR" ]]; then
    echo "❌ Error: Built jar not found at $BUILD_JAR"
    exit 1
fi

JAR_SIZE=$(ls -lh "$BUILD_JAR" | awk '{print $5}')
echo "  ✓ Build ready: spycraft-1.0.0.jar ($JAR_SIZE)"

# 3. Locate Prism Launcher 1.21.1 instance(s) on Steam Deck
echo "[3/4] Finding Prism Launcher instances on Steam Deck..."
MOD_DIRS=$(ssh "$DECK_HOST" "find '$DECK_INSTANCES_DIR' -maxdepth 4 -name 'mods' -type d 2>/dev/null" || true)

if [[ -z "$MOD_DIRS" ]]; then
    echo "⚠️ Warning: No existing 'mods' folder found. Creating default 1.21.1 mods folder..."
    DEFAULT_MODS="$DECK_INSTANCES_DIR/1.21.1/minecraft/mods"
    ssh "$DECK_HOST" "mkdir -p '$DEFAULT_MODS'"
    TARGET_DIRS="$DEFAULT_MODS"
else
    # Filter for 1.21.1 instances, or use all found
    FILTERED=$(echo "$MOD_DIRS" | grep "1.21.1" || echo "$MOD_DIRS")
    TARGET_DIRS="$FILTERED"
fi

# 4. Transfer the JAR
echo "[4/4] Deploying to Steam Deck..."
while IFS= read -r dest_dir; do
    [[ -z "$dest_dir" ]] && continue
    echo "  -> Copying to: $dest_dir"
    scp -o ConnectTimeout=5 "$BUILD_JAR" "$DECK_HOST:$dest_dir/spycraft-1.0.0.jar"
done <<< "$TARGET_DIRS"

# Also copy to local harness server directory for HTTP download
cp "$BUILD_JAR" "$SPYCRAFT_DIR/tools/asset-harness/spycraft-1.0.0.jar" 2>/dev/null || true

# Trigger Steam Deck desktop notification if display session exists
ssh "$DECK_HOST" "DISPLAY=:0 DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/1000/bus notify-send -i input-gaming 'SpyCraft Updated!' 'Latest spycraft-1.0.0.jar installed into Prism Launcher.' 2>/dev/null || true"

echo "============================================================"
echo " ✅ SpyCraft successfully deployed to Steam Deck!"
echo "    Start Minecraft in Prism Launcher on your Steam Deck to play."
echo "============================================================"
