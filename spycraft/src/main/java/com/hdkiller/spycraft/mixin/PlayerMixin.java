package com.hdkiller.spycraft.mixin;

import com.hdkiller.spycraft.item.ModItems;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "isScoping", at = @At("HEAD"), cancellable = true)
    private void erikcraft$isScoping(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (player.hasEffect(com.hdkiller.spycraft.effect.ModEffects.DRONE_PILOTING)) {
            cir.setReturnValue(false);
            return;
        }
        if (player.isUsingItem() && (player.getUseItem().is(ModItems.SNIPER_RIFLE) || player.getUseItem().is(ModItems.BINOCULARS))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void erikcraft$cancelGrappleFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (com.hdkiller.spycraft.item.GrapplingHookGunItem.hasRecentGrappleProtection(player.getUUID())) {
            com.hdkiller.spycraft.item.GrapplingHookGunItem.clearGrappleProtection(player.getUUID());
            cir.setReturnValue(false);
        }
    }
}
