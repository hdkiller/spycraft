package com.hdkiller.spycraft;

import com.hdkiller.spycraft.drone.ReconDroneManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Recon Drone Battery & Telemetry Tests")
class ReconDroneTelemetryTest {

    @ParameterizedTest(name = "Battery ticks {0}/{1} -> {2}%")
    @CsvSource({
        "1200, 1200, 100",
        "600, 1200, 50",
        "300, 1200, 25",
        "0, 1200, 0",
        "120, 1200, 10"
    })
    void testBatteryPercentageCalculation(int remainingTicks, int maxTicks, int expectedPct) {
        int pct = Math.max(0, (int) (((float) remainingTicks / maxTicks) * 100));
        assertEquals(expectedPct, pct);
    }

    @ParameterizedTest(name = "{0} ticks -> {1} seconds")
    @CsvSource({
        "1200, 60",
        "1181, 60",
        "1180, 59",
        "20, 1",
        "1, 1",
        "0, 0"
    })
    void testSecondsRemainingCalculation(int ticks, int expectedSecs) {
        int secs = (ticks + 19) / 20;
        assertEquals(expectedSecs, secs);
    }

    @Test
    @DisplayName("DroneSession constructor initialization and defaults")
    void testDroneSessionState() {
        UUID playerUuid = UUID.randomUUID();
        UUID droneId = UUID.randomUUID();
        Vec3 launchPos = new Vec3(10.5, 70.0, -25.5);
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));

        ReconDroneManager.DroneSession session = new ReconDroneManager.DroneSession(
            playerUuid, droneId, launchPos, 45.0f, -10.0f, dim, 1200, 5
        );

        assertEquals(playerUuid, session.playerUuid);
        assertEquals(droneId, session.droneItemId);
        assertEquals(launchPos, session.launchPos);
        assertEquals(45.0f, session.launchYaw);
        assertEquals(-10.0f, session.launchPitch);
        assertEquals(dim, session.dimension);
        assertEquals(1200, session.maxTicks);
        assertEquals(1200, session.ticksRemaining);
        assertEquals(5, session.dartsRemaining);
        assertTrue(session.scannedMobs.isEmpty());
        assertNull(session.laserTaggedMob);
    }

    @Test
    @DisplayName("SleepingMobState record behavior")
    void testSleepingMobState() {
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));
        ReconDroneManager.SleepingMobState state = new ReconDroneManager.SleepingMobState(dim, 300);

        assertEquals(dim, state.dimension());
        assertEquals(300, state.ticksRemaining());
    }
}
