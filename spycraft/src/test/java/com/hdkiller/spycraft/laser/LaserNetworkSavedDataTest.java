package com.hdkiller.spycraft.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LaserNetworkSavedDataTest {
    @Test
    void preservesOwnersDimensionsCoordinatesAndActiveStateAcrossReload() {
        UUID owner = UUID.randomUUID();
        LaserNetworkSavedData original = new LaserNetworkSavedData();
        var network = new LaserForcefieldManager.ForcefieldNetwork(owner);
        var overworld = new LaserForcefieldManager.LaserTrap(owner, Level.OVERWORLD);
        var nether = new LaserForcefieldManager.LaserTrap(owner, Level.NETHER);
        BlockPos shared = new BlockPos(-30, 64, 40);
        overworld.pylons.add(shared);
        overworld.pylons.add(shared.east(8));
        overworld.active = true;
        nether.pylons.add(shared);
        nether.pylons.add(shared.north(4));
        network.traps.add(overworld);
        network.traps.add(nether);
        original.networks.put(owner, network);

        var restored = LaserNetworkSavedData.load(original.save(new CompoundTag(), null), null).networks.get(owner);
        assertEquals(2, restored.traps.size());
        var restoredOverworld = restored.findNearestTrap(Level.OVERWORLD, shared, 1);
        var restoredNether = restored.findNearestTrap(Level.NETHER, shared, 1);
        assertEquals(overworld.id, restoredOverworld.id);
        assertEquals(nether.id, restoredNether.id);
        assertEquals(overworld.pylons, restoredOverworld.pylons);
        assertEquals(nether.pylons, restoredNether.pylons);
        assertTrue(restoredOverworld.active);
        assertFalse(restoredNether.active);
        assertNull(restored.findNearestTrap(Level.END, shared, 64));
    }

}
