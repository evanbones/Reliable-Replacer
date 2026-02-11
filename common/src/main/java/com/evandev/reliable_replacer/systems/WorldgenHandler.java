package com.evandev.reliable_replacer.systems;

import com.evandev.reliable_replacer.api.IProcessedChunk;
import com.evandev.reliable_replacer.logic.impl.LiveReplacementContext;
import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.logic.ChunkRuleCache;
import com.evandev.reliable_replacer.logic.RuleManager;
import com.evandev.reliable_replacer.data.ReplacementResult;
import com.evandev.reliable_replacer.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.LevelData;

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

        BlockUtil.processChunkBlocks(chunk, (pos, original) -> {
            ctx.setPos(pos);
            ReplacementResult result = RuleManager.getReplacementResult(original, ctx, false);

            if (result != null) {
                BlockState replacement = result.state();
                if (replacement != original) {
                    chunk.setBlockState(pos, replacement, false);
                }
            }
        });
    }
}