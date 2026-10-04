package com.hdkiller.spycraft.client;

import com.hdkiller.spycraft.network.BinocularScanPayload;

/** Client HUD snapshot of the server's target-specific dwell lock. */
public final class BinocularScanState {
    private static BinocularScanPayload state = new BinocularScanPayload(-1, 0, false);

    public static void update(BinocularScanPayload payload) { state = payload; }
    public static void clear() { state = new BinocularScanPayload(-1, 0, false); }
    public static int dwellTicks(int targetId) { return state.targetId() == targetId ? state.dwellTicks() : 0; }
    public static boolean isLocked(int targetId) { return state.targetId() == targetId && state.locked(); }
}
