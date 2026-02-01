package com.evandev.reliable_replacer.util;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.config.RuleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class RetrogenHandler {

    public static void processChunk(LevelChunk chunk) {
        IProcessedChunk access = (IProcessedChunk) chunk;
        if (!ModConfig.get().enabled || (access.reliableReplacer$hasBeenProcessed() && !access.reliableReplacer$isDirty())) {
            return;
        }

        Level level = chunk.getLevel();
        boolean changed = false;
        LevelChunkSection[] sections = chunk.getSections();

        ChunkRuleCache chunkCache = new ChunkRuleCache(level, chunk.getPos());

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;

            int bottomY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            int startX = chunk.getPos().getMinBlockX();
            int startZ = chunk.getPos().getMinBlockZ();

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = new BlockPos(startX + x, bottomY + y, startZ + z);
                        BlockState original = section.getBlockState(x, y, z);
                        BlockState replacement = RuleManager.getReplacement(original, pos, level, true, false, chunkCache);

                        if (replacement != original) {
                            level.setBlock(pos, replacement, 2);
                            changed = true;
                        }
                    }
                }
            }
        }

        access.reliableReplacer$markProcessed();
        access.reliableReplacer$setDirty(false);

        if (changed && level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(chunk.getPos().getWorldPosition());
        }
    }
}