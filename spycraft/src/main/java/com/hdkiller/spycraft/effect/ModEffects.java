package com.hdkiller.spycraft.effect;

import com.hdkiller.spycraft.SpyCraftMod;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

public class ModEffects {
    public static final Holder<MobEffect> DRONE_PILOTING = register("drone_piloting", new DronePilotingMobEffect());

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        return Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(SpyCraftMod.MOD_ID, name),
            effect
        );
    }

    public static void registerModEffects() {
        SpyCraftMod.LOGGER.info("Registering custom mob effects for " + SpyCraftMod.MOD_ID);
    }
}
