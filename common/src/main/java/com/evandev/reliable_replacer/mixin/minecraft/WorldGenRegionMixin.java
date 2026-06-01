package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.data.ReplacementResult;
import com.evandev.reliable_replacer.logic.RuleManager;
import com.evandev.reliable_replacer.logic.impl.LiveReplacementContext;
import com.evandev.reliable_replacer.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public class WorldGenRegionMixin {

    @Unique
    private final ThreadLocal<Boolean> reliableReplacer$isReplacing = ThreadLocal.withInitial(() -> false);

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void reliableReplacer$onWorldGenSetBlock(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        if (reliableReplacer$isReplacing.get() || !ModConfig.get().enabled) return;

        if (RuleManager.RULES_BY_BLOCK == null || !RuleManager.RULES_BY_BLOCK.containsKey(state.getBlock())) {
            return;
        }

        WorldGenRegion region = (WorldGenRegion) (Object) this;

        LiveReplacementContext ctx = new LiveReplacementContext(region, pos, region.getLevel().getRespawnData().globalPos().pos(), false, null, null);
        ReplacementResult result = RuleManager.getReplacementResult(state, ctx, false);

        if (result != null) {
            BlockState replacement = result.state();
            boolean hasAdditionalBlocks = result.additionalBlocks() != null && !result.additionalBlocks().isEmpty();
            boolean hasCustomNbt = result.customNbt() != null;
            boolean hasItemReplacements = result.itemReplacements() != null && !result.itemReplacements().isEmpty();

            if (!replacement.equals(state) || hasAdditionalBlocks || hasCustomNbt || hasItemReplacements) {
                int cx = pos.getX() >> 4;
                int cz = pos.getZ() >> 4;

                if (!region.hasChunk(cx, cz)) return;

                reliableReplacer$isReplacing.set(true);
                try {
                    ChunkAccess currentChunk = region.getChunk(cx, cz);

                    if (!replacement.equals(state) || hasCustomNbt || hasItemReplacements) {
                        BlockUtil.safeSetBlock(region, currentChunk, pos, replacement, result.keepNbt(), result.customNbt(), result.itemReplacements());
                    }

                    if (hasAdditionalBlocks) {
                        for (var entry : result.additionalBlocks().entrySet()) {
                            BlockPos addPos = entry.getKey();
                            int addCx = addPos.getX() >> 4;
                            int addCz = addPos.getZ() >> 4;

                            if (region.hasChunk(addCx, addCz)) {
                                CompoundTag addNbt = result.additionalNbt().get(addPos);
                                ChunkAccess addChunk = region.getChunk(addCx, addCz);
                                BlockUtil.safeSetBlock(region, addChunk, addPos, entry.getValue(), false, addNbt, null);
                            }
                        }
                    }

                    cir.setReturnValue(true);
                } finally {
                    reliableReplacer$isReplacing.set(false);
                }
            }
        }
    }
}