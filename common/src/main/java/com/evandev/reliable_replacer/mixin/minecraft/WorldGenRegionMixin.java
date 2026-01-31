package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.config.RuleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public class WorldGenRegionMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void reliableReplacer$onSetBlock(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.get().enabled) {
            return;
        }

        WorldGenRegion region = (WorldGenRegion) (Object) this;

        BlockState replacement = RuleManager.getReplacement(state, pos, region, false);

        if (replacement != state) {
            cir.setReturnValue(region.setBlock(pos, replacement, flags, recursionLeft));
        }
    }
}