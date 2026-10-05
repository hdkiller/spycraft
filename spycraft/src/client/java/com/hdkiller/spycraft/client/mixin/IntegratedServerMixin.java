package com.hdkiller.spycraft.client.mixin;

import com.hdkiller.spycraft.SpyCraftMod;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Inject(method = "publishServer", at = @At("HEAD"))
    private void spycraft$allowOfflineLan(GameType gameType, boolean cheats, int port, CallbackInfoReturnable<Boolean> cir) {
        ((MinecraftServer) (Object) this).setUsesAuthentication(false);
        SpyCraftMod.LOGGER.info("SpyCraft: Enabled offline authentication for local LAN multiplayer guests.");
    }
}
