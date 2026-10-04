package com.hdkiller.spycraft;

import com.hdkiller.spycraft.laser.LaserForcefieldManager;
import com.hdkiller.spycraft.mission.SpyBaseMissionBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditRegressionTest {
    @Test
    void missionRequiresEntireStructureToFitInsideBuildLimits() {
        assertTrue(SpyBaseMissionBuilder.fitsBuildHeight(224, 96, -64, 320));
        assertFalse(SpyBaseMissionBuilder.fitsBuildHeight(225, 96, -64, 320));
        assertTrue(SpyBaseMissionBuilder.fitsBuildHeight(-64, 96, -64, 320));
        assertFalse(SpyBaseMissionBuilder.fitsBuildHeight(-65, 96, -64, 320));
        assertFalse(SpyBaseMissionBuilder.fitsBuildHeight(50, 96, 0, 128));
        assertFalse(SpyBaseMissionBuilder.fitsBuildHeight(Integer.MAX_VALUE, 96, -64, 320));
    }

    @Test
    void slopedLaserCollisionFollowsRenderedHeight() {
        assertEquals(64, LaserForcefieldManager.interpolatedHeight(64, 80, 0));
        assertEquals(72, LaserForcefieldManager.interpolatedHeight(64, 80, 0.5));
        assertEquals(80, LaserForcefieldManager.interpolatedHeight(64, 80, 1));
        assertEquals(72, LaserForcefieldManager.interpolatedHeight(80, 64, 0.5));
    }
}
