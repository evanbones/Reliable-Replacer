package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.Constants;
import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.data.ReplacementResult;
import com.evandev.reliable_replacer.logic.ChunkRuleCache;
import com.evandev.reliable_replacer.logic.RuleManager;
import com.evandev.reliable_replacer.logic.impl.LiveReplacementContext;
import com.evandev.reliable_replacer.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(WorldGenRegion.class)
public class WorldGenRegionMixin {

    @Unique
    private final ThreadLocal<Boolean> reliableReplacer$isReplacing = ThreadLocal.withInitial(() -> false);

    @Unique
    private final Map<Long, ChunkRuleCache> reliableReplacer$ruleCaches = new HashMap<>();

    @Unique
    private boolean reliableReplacer$canWrite(WorldGenRegion region, BlockPos target) {
        WorldGenRegionAccessor accessor = (WorldGenRegionAccessor) region;
        int writeRadius = accessor.reliableReplacer$getWriteRadiusCutoff();
        ChunkPos center = region.getCenter();

        if (Math.abs(center.x - SectionPos.blockToSectionCoord(target.getX())) > writeRadius
                || Math.abs(center.z - SectionPos.blockToSectionCoord(target.getZ())) > writeRadius) {
            return false;
        }

        ChunkAccess centerChunk = accessor.reliableReplacer$getCenterChunk();
        if (centerChunk.isUpgrading()) {
            LevelHeightAccessor heightAccessor = centerChunk.getHeightAccessorForGeneration();
            return target.getY() >= heightAccessor.getMinBuildHeight() && target.getY() < heightAccessor.getMaxBuildHeight();
        }

        return true;
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void reliableReplacer$onWorldGenSetBlock(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        if (reliableReplacer$isReplacing.get() || !ModConfig.get().enabled) return;

        if (RuleManager.RULES_BY_BLOCK == null || !RuleManager.RULES_BY_BLOCK.containsKey(state.getBlock())) {
            return;
        }

        WorldGenRegion region = (WorldGenRegion) (Object) this;

        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;

        ChunkRuleCache ruleCache = reliableReplacer$ruleCaches.computeIfAbsent(
                ChunkPos.asLong(cx, cz), key -> new ChunkRuleCache(region, new ChunkPos(cx, cz)));

        LiveReplacementContext ctx = new LiveReplacementContext(region, pos, region.getLevel().getSharedSpawnPos(), false, null, ruleCache);
        ReplacementResult result = RuleManager.getReplacementResult(state, ctx, false);

        if (result != null) {
            BlockState replacement = result.state();
            boolean hasAdditionalBlocks = result.additionalBlocks() != null && !result.additionalBlocks().isEmpty();
            boolean hasCustomNbt = result.customNbt() != null;
            boolean hasItemReplacements = result.itemReplacements() != null && !result.itemReplacements().isEmpty();

            if (!replacement.equals(state) || hasAdditionalBlocks || hasCustomNbt || hasItemReplacements) {
                if (!reliableReplacer$canWrite(region, pos)) return;

                reliableReplacer$isReplacing.set(true);
                try {
                    ChunkAccess currentChunk = region.getChunk(cx, cz);

                    if (!replacement.equals(state) || hasCustomNbt || hasItemReplacements) {
                        BlockUtil.safeSetBlock(region, currentChunk, pos, replacement, result.keepNbt(), result.customNbt(), result.itemReplacements());
                    }

                    if (hasAdditionalBlocks) {
                        for (var entry : result.additionalBlocks().entrySet()) {
                            BlockPos addPos = entry.getKey();

                            if (reliableReplacer$canWrite(region, addPos)) {
                                CompoundTag addNbt = result.additionalNbt().get(addPos);
                                ChunkAccess addChunk = region.getChunk(addPos.getX() >> 4, addPos.getZ() >> 4);
                                BlockUtil.safeSetBlock(region, addChunk, addPos, entry.getValue(), false, addNbt, null);
                            } else {
                                Constants.LOG.debug("Skipping additional block at {} during world generation: outside the writable chunk radius of {}.",
                                        addPos, region.getCenter());
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
