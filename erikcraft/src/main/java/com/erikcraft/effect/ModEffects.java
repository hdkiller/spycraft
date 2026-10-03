package com.erikcraft.effect;

import com.erikcraft.ErikCraftMod;
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
            ResourceLocation.fromNamespaceAndPath(ErikCraftMod.MOD_ID, name),
            effect
        );
    }

    public static void registerModEffects() {
        ErikCraftMod.LOGGER.info("Registering custom mob effects for " + ErikCraftMod.MOD_ID);
    }
}
