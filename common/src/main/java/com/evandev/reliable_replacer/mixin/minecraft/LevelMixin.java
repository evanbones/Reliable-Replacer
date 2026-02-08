package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.config.RuleManager;
import com.evandev.reliable_replacer.data.ReplacementResult;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Unique
    private static final ThreadLocal<Boolean> reliableReplacer$isReplacing = ThreadLocal.withInitial(() -> false);

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", at = @At("HEAD"), cancellable = true)
    private void onSetBlock(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
        if (!RuleManager.HAS_LIVE_RULES) return;
        if (reliableReplacer$isReplacing.get()) return;

        Level level = (Level) (Object) this;
        if (level.isClientSide || !ModConfig.get().enabled) return;

        ReplacementResult result = RuleManager.getReplacementResult(state, pos, level, false, true, null);

        if (result != null) {
            BlockState replacement = result.state();

            if (!replacement.equals(state)) {
                reliableReplacer$isReplacing.set(true);
                try {
                    CompoundTag nbtData = null;
                    if (result.keepNbt()) {
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be != null) {
                            nbtData = be.saveWithoutMetadata();
                        }
                    }

                    boolean success = level.setBlock(pos, replacement, flags);

                    if (success && nbtData != null) {
                        BlockEntity newBlockEntity = level.getBlockEntity(pos);
                        if (newBlockEntity != null) {
                            newBlockEntity.load(nbtData);
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