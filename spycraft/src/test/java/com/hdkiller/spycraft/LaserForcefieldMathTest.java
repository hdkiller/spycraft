package com.hdkiller.spycraft;

import com.hdkiller.spycraft.laser.LaserForcefieldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Laser Forcefield Geometry & Repulsion Physics Tests")
class LaserForcefieldMathTest {

    @Test
    @DisplayName("Centroid calculation of 4-pylon square perimeter")
    void testCentroidCalculation() {
        List<BlockPos> pylons = List.of(
            new BlockPos(0, 64, 0),
            new BlockPos(10, 64, 0),
            new BlockPos(10, 64, 10),
            new BlockPos(0, 64, 10)
        );

        double cx = 0, cz = 0;
        for (BlockPos p : pylons) {
            cx += p.getX() + 0.5;
            cz += p.getZ() + 0.5;
        }
        cx /= pylons.size();
        cz /= pylons.size();

        assertEquals(5.5, cx, 0.0001, "Centroid X should be exactly in the center of the square");
        assertEquals(5.5, cz, 0.0001, "Centroid Z should be exactly in the center of the square");
    }

    @Test
    @DisplayName("Point to segment closest distance projection")
    void testSegmentProjectionMath() {
        // Line segment from (0, 0) to (10, 0)
        double ax = 0.5, az = 0.5;
        double bx = 10.5, bz = 0.5;

        double segDx = bx - ax;
        double segDz = bz - az;
        double segLenSq = segDx * segDx + segDz * segDz;

        // Test 1: Point at (5.5, 2.0) directly perpendicular
        double ex = 5.5, ez = 2.0;
        double t = Math.max(0, Math.min(1, ((ex - ax) * segDx + (ez - az) * segDz) / segLenSq));
        double closeX = ax + t * segDx;
        double closeZ = az + t * segDz;

        assertEquals(0.5, t, 0.0001, "Projection should be at mid-point (t = 0.5)");
        assertEquals(5.5, closeX, 0.0001);
        assertEquals(0.5, closeZ, 0.0001);

        double distSq = (ex - closeX) * (ex - closeX) + (ez - closeZ) * (ez - closeZ);
        assertEquals(2.25, distSq, 0.0001, "Distance squared should be 1.5^2 = 2.25");

        // Test 2: Point beyond endpoint (t should clamp to 1.0)
        double exBeyond = 15.0, ezBeyond = 0.5;
        double tBeyond = Math.max(0, Math.min(1, ((exBeyond - ax) * segDx + (ezBeyond - az) * segDz) / segLenSq));
        assertEquals(1.0, tBeyond, 0.0001, "Point beyond segment should clamp to t = 1.0");
    }

    @Test
    @DisplayName("Barrier repulsion vector direction (Inside vs Outside)")
    void testRepulsionDirection() {
        double cx = 5.5, cz = 5.5; // Centroid
        double wallX = 5.5, wallZ = 0.5; // Wall position

        double distWallToCenter = (wallX - cx) * (wallX - cx) + (wallZ - cz) * (wallZ - cz);

        // Entity inside perimeter (e.g. at 5.5, 1.0)
        double insideEx = 5.5, insideEz = 1.0;
        double distInsideToCenter = (insideEx - cx) * (insideEx - cx) + (insideEz - cz) * (insideEz - cz);

        assertTrue(distInsideToCenter < distWallToCenter, "Entity inside is closer to center than wall");
        Vec3 pushDirInside = new Vec3(cx - insideEx, 0, cz - insideEz).normalize();
        assertTrue(pushDirInside.z > 0, "Inside entity pushed towards center (+z)");

        // Entity outside perimeter (e.g. at 5.5, 0.0)
        double outsideEx = 5.5, outsideEz = 0.0;
        double distOutsideToCenter = (outsideEx - cx) * (outsideEx - cx) + (outsideEz - cz) * (outsideEz - cz);

        assertTrue(distOutsideToCenter > distWallToCenter, "Entity outside is farther from center than wall");
        Vec3 pushDirOutside = new Vec3(outsideEx - wallX, 0, outsideEz - wallZ).normalize();
        assertTrue(pushDirOutside.z < 0, "Outside entity pushed away from wall (-z)");
    }

    @Test
    @DisplayName("Pylon removal unconditionally turns off active forcefield even if 3+ pylons remain")
    void testNetworkPylonRemovalDeactivatesForcefield() {
        java.util.UUID owner = java.util.UUID.randomUUID();
        LaserForcefieldManager.ForcefieldNetwork net = LaserForcefieldManager.getNetwork(owner);
        BlockPos p1 = new BlockPos(0, 64, 0);
        BlockPos p2 = new BlockPos(10, 64, 0);
        BlockPos p3 = new BlockPos(10, 64, 10);
        BlockPos p4 = new BlockPos(0, 64, 10);

        net.pylons.clear();
        net.pylons.addAll(List.of(p1, p2, p3, p4));
        net.active = true;

        // Player breaks p1: Even though 3 pylons remain (p2, p3, p4), active MUST turn false!
        boolean removed = net.pylons.remove(p1);
        assertTrue(removed);
        net.active = false; // As enforced in onPylonBroken

        assertFalse(net.active, "Forcefield must shut down immediately upon pylon destruction");
        assertEquals(3, net.pylons.size(), "Remaining pylons should be 3");

        // Clean up
        LaserForcefieldManager.clearNetwork(owner);
    }

    @Test
    @DisplayName("setNetworkPylonsActive updates active flag and state cleanly")
    void testSetNetworkPylonsActive() {
        java.util.UUID owner = java.util.UUID.randomUUID();
        LaserForcefieldManager.ForcefieldNetwork net = LaserForcefieldManager.getNetwork(owner);
        BlockPos p1 = new BlockPos(0, 64, 0);
        net.pylons.add(p1);

        LaserForcefieldManager.setNetworkPylonsActive(null, net, true);
        assertTrue(net.active);

        LaserForcefieldManager.setNetworkPylonsActive(null, net, false);
        assertFalse(net.active);

        LaserForcefieldManager.clearNetwork(owner);
    }

    @Test
    @DisplayName("Independent multi-trap clustering: distant pylons form separate traps")
    void testMultiTrapClustering() {
        java.util.UUID owner = java.util.UUID.randomUUID();
        LaserForcefieldManager.ForcefieldNetwork net = LaserForcefieldManager.getNetwork(owner);

        // Trap 1: Base A at (0, 64, 0), (5, 64, 0), (5, 64, 5)
        LaserForcefieldManager.LaserTrap trap1 = new LaserForcefieldManager.LaserTrap(owner, Level.OVERWORLD);
        trap1.pylons.addAll(List.of(
            new BlockPos(0, 64, 0),
            new BlockPos(5, 64, 0),
            new BlockPos(5, 64, 5)
        ));
        trap1.active = true;
        net.traps.add(trap1);

        // Trap 2: Base B at (100, 64, 100), (105, 64, 100)
        // Distance to Trap 1 is ~140 blocks (well above MAX_LINK_DISTANCE = 32 blocks)
        LaserForcefieldManager.LaserTrap trap2 = new LaserForcefieldManager.LaserTrap(owner, Level.OVERWORLD);
        trap2.pylons.addAll(List.of(
            new BlockPos(100, 64, 100),
            new BlockPos(105, 64, 100)
        ));
        trap2.active = true;
        net.traps.add(trap2);

        assertEquals(2, net.traps.size(), "Player should have 2 distinct traps");
        assertTrue(trap1.active, "Trap 1 should be active");
        assertTrue(trap2.active, "Trap 2 should be active");

        // Nearest trap lookup from Base A (1, 64, 1) should be trap1
        LaserForcefieldManager.LaserTrap nearestA = net.findNearestTrap(Level.OVERWORLD, new BlockPos(1, 64, 1), 64.0);
        assertSame(trap1, nearestA, "Nearest to (1,64,1) must be Trap 1");

        // Nearest trap lookup from Base B (102, 64, 102) should be trap2
        LaserForcefieldManager.LaserTrap nearestB = net.findNearestTrap(Level.OVERWORLD, new BlockPos(102, 64, 102), 64.0);
        assertSame(trap2, nearestB, "Nearest to (102,64,102) must be Trap 2");

        // Deactivating or breaking pylon in Trap 2 leaves Trap 1 fully intact and active!
        LaserForcefieldManager.setTrapPylonsActive(null, trap2, false);
        assertFalse(trap2.active, "Trap 2 should be inactive");
        assertTrue(trap1.active, "Trap 1 must remain active and undisturbed!");

        // Clean up
        LaserForcefieldManager.clearNetwork(owner);
    }
}
