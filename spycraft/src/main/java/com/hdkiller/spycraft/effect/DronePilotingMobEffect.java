package com.hdkiller.spycraft.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class DronePilotingMobEffect extends MobEffect {
    public DronePilotingMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00FFCC); // Cyan tactical drone color
    }
}
