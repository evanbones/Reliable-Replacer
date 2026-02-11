package com.evandev.reliable_replacer.systems;

import com.evandev.reliable_replacer.api.IProcessedChunk;
import com.evandev.reliable_replacer.logic.impl.LiveReplacementContext;
import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.logic.ChunkRuleCache;
import com.evandev.reliable_replacer.logic.RuleManager;
import com.evandev.reliable_replacer.data.ReplacementResult;
import com.evandev.reliable_replacer.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelData;

import java.util.concurrent.atomic.AtomicBoolean;

public class RetrogenHandler {

    public static void processChunk(LevelChunk chunk) {
        IProcessedChunk access = (IProcessedChunk) chunk;
        if (!ModConfig.get().enabled || (access.reliableReplacer$hasBeenProcessed() && !access.reliableReplacer$isDirty())) {
            return;
        }

        Level level = chunk.getLevel();
        LevelData levelData = level.getLevelData();
        BlockPos spawnPos = new BlockPos(levelData.getXSpawn(), levelData.getYSpawn(), levelData.getZSpawn());

        ChunkRuleCache cache = new ChunkRuleCache(level, chunk.getPos());
        LiveReplacementContext ctx = new LiveReplacementContext(level, new BlockPos(0, 0, 0), spawnPos, true, chunk, cache);

        AtomicBoolean changed = new AtomicBoolean(false);

        BlockUtil.processChunkBlocks(chunk, (pos, original) -> {
            ctx.setPos(pos);
            ReplacementResult result = RuleManager.getReplacementResult(original, ctx, false);

            if (result != null) {
                BlockState replacement = result.state();
                if (replacement != original) {
                    BlockUtil.swapBlockWithNbt(level, pos, result, 3);
                    changed.set(true);
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