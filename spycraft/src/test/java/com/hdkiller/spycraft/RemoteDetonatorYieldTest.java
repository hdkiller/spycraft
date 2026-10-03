package com.hdkiller.spycraft;

import com.hdkiller.spycraft.item.RemoteDetonatorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Remote Detonator Yield & Charge Location Tests")
class RemoteDetonatorYieldTest {

    @ParameterizedTest(name = "Current level {0} cycles to {1}")
    @CsvSource({
        "0, 1",
        "1, 2",
        "2, 3",
        "3, 1",
        "4, 1"
    })
    void testYieldLevelCycling(int currentLevel, int expectedNewLevel) {
        int newLevel = currentLevel >= 3 ? 1 : currentLevel + 1;
        assertEquals(expectedNewLevel, newLevel);
    }

    @ParameterizedTest(name = "Yield level {0} produces explosion power {1}")
    @CsvSource({
        "1, 4.5",
        "2, 9.0",
        "3, 18.0"
    })
    void testExplosionPowerFormula(int yieldLevel, float expectedPower) {
        float power = yieldLevel == 1 ? 4.5f : (yieldLevel == 2 ? 9.0f : 18.0f);
        assertEquals(expectedPower, power, 0.001f);
    }

    @Test
    @DisplayName("ChargeLocation record equality across dimensions")
    void testChargeLocationEquality() {
        ResourceKey<Level> overworld = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));
        ResourceKey<Level> nether = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether"));

        BlockPos pos1 = new BlockPos(100, 64, 200);
        BlockPos pos2 = new BlockPos(100, 64, 200);
        BlockPos pos3 = new BlockPos(101, 64, 200);

        RemoteDetonatorItem.ChargeLocation loc1 = new RemoteDetonatorItem.ChargeLocation(overworld, pos1);
        RemoteDetonatorItem.ChargeLocation loc2 = new RemoteDetonatorItem.ChargeLocation(overworld, pos2);
        RemoteDetonatorItem.ChargeLocation locNether = new RemoteDetonatorItem.ChargeLocation(nether, pos1);
        RemoteDetonatorItem.ChargeLocation locDifferentPos = new RemoteDetonatorItem.ChargeLocation(overworld, pos3);

        assertEquals(loc1, loc2, "Same dimension and BlockPos should be equal");
        assertEquals(loc1.hashCode(), loc2.hashCode(), "HashCodes must match for map lookups");
        assertNotEquals(loc1, locNether, "Charges in different dimensions must not collide");
        assertNotEquals(loc1, locDifferentPos, "Charges at different BlockPos must not collide");
    }
}
