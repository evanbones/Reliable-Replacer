package com.evandev.reliable_replacer.systems;

import com.evandev.reliable_replacer.api.IProcessedChunk;
import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.data.ReplacementResult;
import com.evandev.reliable_replacer.logic.ChunkRuleCache;
import com.evandev.reliable_replacer.logic.RuleManager;
import com.evandev.reliable_replacer.logic.impl.LiveReplacementContext;
import com.evandev.reliable_replacer.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class RetrogenHandler {

    public static void processChunk(LevelChunk chunk) {
        IProcessedChunk access = (IProcessedChunk) chunk;
        if (!ModConfig.get().enabled || (access.reliableReplacer$hasBeenProcessed() && !access.reliableReplacer$isDirty())) {
            return;
        }

        Level level = chunk.getLevel();
        BlockPos spawnPos = level.getSharedSpawnPos();

        ChunkRuleCache cache = new ChunkRuleCache(level, chunk.getPos());
        LiveReplacementContext ctx = new LiveReplacementContext(level, new BlockPos(0, 0, 0), spawnPos, true, chunk, cache);

        AtomicBoolean changed = new AtomicBoolean(false);
        Set<BlockPos> modifiedPositions = new HashSet<>();

        BlockUtil.processChunkBlocks(chunk, (pos, original) -> {
            if (modifiedPositions.contains(pos)) return;

            ctx.setPos(pos);
            ReplacementResult result = RuleManager.getReplacementResult(original, ctx, false);

            if (result != null) {
                BlockState replacement = result.state();
                boolean hasCustomNbt = result.customNbt() != null;
                boolean hasItemReplacements = result.itemReplacements() != null && !result.itemReplacements().isEmpty();

                if (replacement != original || hasCustomNbt || hasItemReplacements) {
                    BlockUtil.swapBlockWithNbt(level, pos, replacement, result.keepNbt(), result.customNbt(), result.itemReplacements(), 50);
                    changed.set(true);
                    modifiedPositions.add(pos.immutable());
                }

                if (result.additionalBlocks() != null && !result.additionalBlocks().isEmpty()) {
                    for (var entry : result.additionalBlocks().entrySet()) {
                        BlockPos addPos = entry.getKey();
                        if (!level.isLoaded(addPos)) continue;

                        BlockState addState = entry.getValue();
                        CompoundTag addNbt = result.additionalNbt().get(addPos);
                        BlockUtil.swapBlockWithNbt(level, addPos, addState, false, addNbt, null, 50);
                        changed.set(true);
                        modifiedPositions.add(addPos.immutable());
                    }
                }
            }
        });

        access.reliableReplacer$markProcessed();
        access.reliableReplacer$setDirty(false);

        if (changed.get() && level instanceof ServerLevel) {
            chunk.setUnsaved(true);
        }
    }
}