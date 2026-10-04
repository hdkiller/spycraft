package com.hdkiller.spycraft;

import com.hdkiller.spycraft.item.ReconDroneItem;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DroneIdentityTest {
    @BeforeAll
    static void initializeRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void identifiesOnlyTheLaunchedItemAndPreservesItsResources() {
        ItemStack launched = new ItemStack(Items.STICK);
        ItemStack spare = new ItemStack(Items.STICK);
        ReconDroneItem.setBattery(launched, 420);
        ReconDroneItem.setDarts(launched, 2);
        var launchedId = ReconDroneItem.getOrCreateDroneId(launched);
        var spareId = ReconDroneItem.getOrCreateDroneId(spare);
        assertNotEquals(launchedId, spareId);
        assertEquals(launchedId, ReconDroneItem.getOrCreateDroneId(launched));
        assertTrue(ReconDroneItem.hasDroneId(launched, launchedId));
        assertFalse(ReconDroneItem.hasDroneId(spare, launchedId));
        assertEquals(420, ReconDroneItem.getBattery(launched));
        assertEquals(2, ReconDroneItem.getDarts(launched));
        assertTrue(ReconDroneItem.hasDroneId(launched.copy(), launchedId), "Identity survives inventory stack copies");
    }
}
