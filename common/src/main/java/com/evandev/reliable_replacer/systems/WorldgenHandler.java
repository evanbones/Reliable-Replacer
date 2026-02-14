package com.evandev.reliable_replacer.systems;

import com.evandev.reliable_replacer.api.IProcessedChunk;
import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.data.ReplacementResult;
import com.evandev.reliable_replacer.logic.ChunkRuleCache;
import com.evandev.reliable_replacer.logic.RuleManager;
import com.evandev.reliable_replacer.logic.impl.LiveReplacementContext;
import com.evandev.reliable_replacer.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.LevelData;

import java.util.HashSet;
import java.util.Set;

public class WorldgenHandler {

    public static void processChunk(LevelAccessor levelAccessor, ChunkAccess chunk) {
        if (!ModConfig.get().enabled) return;

        IProcessedChunk access = (IProcessedChunk) chunk;
        if (access.reliableReplacer$hasBeenProcessed()) return;
        access.reliableReplacer$markProcessed();

        ChunkPos chunkPos = chunk.getPos();
        LevelData levelData = levelAccessor.getLevelData();
        BlockPos spawnPos = new BlockPos(levelData.getSpawnPos());

        ChunkRuleCache cache = new ChunkRuleCache(levelAccessor, chunkPos);
        LiveReplacementContext ctx = new LiveReplacementContext(levelAccessor, new BlockPos(0, 0, 0), spawnPos, false, chunk, cache);

        Set<BlockPos> modifiedPositions = new HashSet<>();

        BlockUtil.processChunkBlocks(chunk, (pos, original) -> {
            if (modifiedPositions.contains(pos)) return;

            ctx.setPos(pos);
            ReplacementResult result = RuleManager.getReplacementResult(original, ctx, false);

            if (result != null) {
                BlockState replacement = result.state();
                if (replacement != original) {
                    BlockUtil.safeSetBlock(levelAccessor, chunk, pos, replacement);
                    modifiedPositions.add(pos.immutable());
                }

                if (result.additionalBlocks() != null && !result.additionalBlocks().isEmpty()) {
                    for (var entry : result.additionalBlocks().entrySet()) {
                        BlockPos addPos = entry.getKey();
                        BlockState addState = entry.getValue();
                        BlockUtil.safeSetBlock(levelAccessor, chunk, addPos, addState);
                        modifiedPositions.add(addPos.immutable());
                    }
                }
            }
        });
    }
}