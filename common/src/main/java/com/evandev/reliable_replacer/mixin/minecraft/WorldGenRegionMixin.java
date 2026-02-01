package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.config.RuleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldGenRegion.class)
public class WorldGenRegionMixin {
    @Unique
    private static final ThreadLocal<Boolean> reliableReplacer$isReplacing = ThreadLocal.withInitial(() -> false);

    @ModifyVariable(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"),
            argsOnly = true
    )
    private BlockState reliableReplacer$globalReplace(BlockState state, BlockPos pos) {
        if (!ModConfig.get().enabled) return state;

        if (reliableReplacer$isReplacing.get()) return state;

        WorldGenRegion region = (WorldGenRegion) (Object) this;

        reliableReplacer$isReplacing.set(true);
        try {
            return RuleManager.getReplacement(state, pos, region, false, false);
        } finally {
            reliableReplacer$isReplacing.set(false);
        }
    }
}