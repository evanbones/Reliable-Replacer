package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.config.RuleManager;
import com.evandev.reliable_replacer.data.ReplacementResult;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public class WorldGenRegionMixin {

    @Unique
    private static final ThreadLocal<Boolean> reliableReplacer$isReplacing = ThreadLocal.withInitial(() -> false);

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onSetBlock(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        if (!RuleManager.HAS_LIVE_RULES) return;
        if (reliableReplacer$isReplacing.get()) return;
        if (!ModConfig.get().enabled) return;

        WorldGenRegion level = (WorldGenRegion) (Object) this;
        ReplacementResult result = RuleManager.getReplacementResult(state, pos, level, false, false, null);

        if (result != null) {
            BlockState replacement = result.state();

            if (!replacement.equals(state)) {
                reliableReplacer$isReplacing.set(true);
                try {
                    CompoundTag nbtData = null;
                    if (result.keepNbt()) {
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be != null) {
                            nbtData = be.saveCustomOnly(level.registryAccess());
                        }
                    }

                    boolean success = level.setBlock(pos, replacement, flags, recursionLeft);

                    if (success && nbtData != null) {
                        BlockEntity newBlockEntity = level.getBlockEntity(pos);
                        if (newBlockEntity != null) {
                            newBlockEntity.loadWithComponents(nbtData, level.registryAccess());
                        }
                    }

                    cir.setReturnValue(success);
                } finally {
                    reliableReplacer$isReplacing.set(false);
                }
            }
        }
    }
}