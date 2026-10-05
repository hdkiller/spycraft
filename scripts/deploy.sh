#!/usr/bin/env bash
# SpyCraft Universal Deploy Tool
# Builds the mod jar and deploys to local macOS Prism Launcher and/or Steam Deck.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SPYCRAFT_DIR="$REPO_ROOT/spycraft"
BUILD_JAR="$SPYCRAFT_DIR/build/libs/spycraft-1.0.0.jar"
DECK_HOST="${STEAMDECK_HOST:-steamdeck}"
DECK_INSTANCES_DIR="/home/deck/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances"
LOCAL_PRISM_DIR="$HOME/Library/Application Support/PrismLauncher/instances"

DO_LOCAL=true
DO_DECK=true
DO_BUILD=true
RUN_TESTS=false

# Parse flags
for arg in "$@"; do
    case "$arg" in
        --local|--prism)
            DO_DECK=false
            ;;
        --deck|--steamdeck)
            DO_LOCAL=false
            ;;
        --skip-build)
            DO_BUILD=false
            ;;
        --test)
            RUN_TESTS=true
            ;;
        --all)
            DO_LOCAL=true
            DO_DECK=true
            ;;
        --help|-h)
            echo "Usage: ./scripts/deploy.sh [OPTIONS]"
            echo "Options:"
            echo "  --all         Deploy to both local Prism Launcher and Steam Deck (default)"
            echo "  --local       Deploy only to local macOS Prism Launcher"
            echo "  --deck        Deploy only to Steam Deck over SSH"
            echo "  --skip-build  Skip gradle build step"
            echo "  --test        Run unit tests before building"
            exit 0
            ;;
    esac
done

echo "============================================================"
echo " 🎮 SpyCraft Universal Deployment"
echo "============================================================"

# 1. Tests (optional)
if [[ "$RUN_TESTS" == true ]]; then
    echo "[1/4] Running tests..."
    cd "$SPYCRAFT_DIR"
    ./gradlew test
    cd "$REPO_ROOT"
fi

# 2. Build JAR
if [[ "$DO_BUILD" == true ]]; then
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
echo "  ✓ Mod jar ready: spycraft-1.0.0.jar ($JAR_SIZE)"

# 3. Deploy to local macOS Prism Launcher
if [[ "$DO_LOCAL" == true ]]; then
    echo "[3/4] Deploying to local Prism Launcher..."
    if [[ -d "$LOCAL_PRISM_DIR" ]]; then
        LOCAL_COUNT=0
        while IFS= read -r mods_dir; do
            [[ -z "$mods_dir" ]] && continue
            INST_NAME=$(basename "$(dirname "$(dirname "$mods_dir")")")
            echo "  -> Installing into local instance: '$INST_NAME'"
            cp "$BUILD_JAR" "$mods_dir/spycraft-1.0.0.jar"
            LOCAL_COUNT=$((LOCAL_COUNT + 1))
        done < <(find "$LOCAL_PRISM_DIR" -maxdepth 3 -type d -name "mods" 2>/dev/null)

        if [[ "$LOCAL_COUNT" -gt 0 ]]; then
            echo "  ✓ Successfully updated $LOCAL_COUNT local Prism Launcher instance(s)!"
        else
            echo "  ⚠️ No local Prism Launcher instances with 'mods' folder found."
        fi
    else
        echo "  ℹ️ Local Prism Launcher directory not found at: $LOCAL_PRISM_DIR"
    fi

    # Also update asset harness if present
    if [[ -d "$SPYCRAFT_DIR/tools/asset-harness" ]]; then
        cp "$BUILD_JAR" "$SPYCRAFT_DIR/tools/asset-harness/spycraft-1.0.0.jar" 2>/dev/null || true
    fi
else
    echo "[3/4] Skipping local deployment."
fi

# 4. Deploy to Steam Deck over SSH
if [[ "$DO_DECK" == true ]]; then
    echo "[4/4] Deploying to Steam Deck ($DECK_HOST)..."
    if ssh -o ConnectTimeout=3 -o BatchMode=yes "$DECK_HOST" "uname -n" >/dev/null 2>&1; then
        echo "  ✓ Connected to Steam Deck over SSH."
        
        DECK_MOD_DIRS=$(ssh "$DECK_HOST" "find '$DECK_INSTANCES_DIR' -maxdepth 4 -name 'mods' -type d 2>/dev/null" || true)
        
        if [[ -z "$DECK_MOD_DIRS" ]]; then
            echo "  ⚠️ No existing 'mods' folder found on Deck. Creating default 1.21.1 mods folder..."
            DEFAULT_MODS="$DECK_INSTANCES_DIR/1.21.1/minecraft/mods"
            ssh "$DECK_HOST" "mkdir -p '$DEFAULT_MODS'"
            DECK_TARGETS="$DEFAULT_MODS"
        else
            # Filter for 1.21.1 instances, or use all found
            FILTERED=$(echo "$DECK_MOD_DIRS" | grep "1.21.1" || echo "$DECK_MOD_DIRS")
            DECK_TARGETS="$FILTERED"
        fi

        while IFS= read -r dest_dir; do
            [[ -z "$dest_dir" ]] && continue
            echo "  -> Copying to Deck: $dest_dir"
            scp -o ConnectTimeout=5 "$BUILD_JAR" "$DECK_HOST:$dest_dir/spycraft-1.0.0.jar"
        done <<< "$DECK_TARGETS"

        # Trigger desktop notification on Steam Deck
        ssh "$DECK_HOST" "DISPLAY=:0 DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/1000/bus notify-send -i input-gaming 'SpyCraft Updated!' 'Latest spycraft-1.0.0.jar installed into Prism Launcher.' 2>/dev/null || true"
        echo "  ✓ Steam Deck deployment complete!"
    else
        echo "  ⚠️ Steam Deck is offline or unreachable over SSH."
        echo "     (Deck sync skipped. Ensure Steam Deck is powered on and connected to Wi-Fi)."
    fi
else
    echo "[4/4] Skipping Steam Deck deployment."
fi

echo "============================================================"
echo " ✅ Deployment completed successfully!"
echo "============================================================"
