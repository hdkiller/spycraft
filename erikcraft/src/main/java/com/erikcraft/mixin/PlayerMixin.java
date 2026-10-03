package com.erikcraft.mixin;

import com.erikcraft.item.ModItems;
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
        if (player.isUsingItem() && player.getUseItem().is(ModItems.SNIPER_RIFLE)) {
            cir.setReturnValue(true);
        }
    }
}
