package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.data.ReplacementResult;
import com.evandev.reliable_replacer.logic.RuleManager;
import com.evandev.reliable_replacer.logic.impl.LiveReplacementContext;
import com.evandev.reliable_replacer.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
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
        if (!ModConfig.get().enabled) return;

        Level level = (Level) (Object) this;
        LevelData levelData = level.getLevelData();
        BlockPos spawnPos = new BlockPos(levelData.getXSpawn(), levelData.getYSpawn(), levelData.getZSpawn());
        LiveReplacementContext ctx = new LiveReplacementContext(level, pos, spawnPos, false, null, null);
        ReplacementResult result = RuleManager.getReplacementResult(state, ctx, true);

        if (result != null) {
            BlockState replacement = result.state();
            boolean hasAdditionalBlocks = result.additionalBlocks() != null && !result.additionalBlocks().isEmpty();

            if (!replacement.equals(state) || hasAdditionalBlocks) {
                reliableReplacer$isReplacing.set(true);
                try {
                    boolean success = false;
                    if (!replacement.equals(state)) {
                        success = BlockUtil.swapBlockWithNbt(level, pos, replacement, result.keepNbt(), flags);
                    }

                    if (hasAdditionalBlocks) {
                        for (var entry : result.additionalBlocks().entrySet()) {
                            level.setBlock(entry.getKey(), entry.getValue(), flags);
                        }
                        if (replacement.equals(state)) success = true;
                    }

                    if (success) {
                        cir.setReturnValue(true);
                    }
                } finally {
                    reliableReplacer$isReplacing.set(false);
                }
            }
        }
    }
}