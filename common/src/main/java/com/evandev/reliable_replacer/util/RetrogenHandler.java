package com.evandev.reliable_replacer.util;

import com.evandev.reliable_replacer.config.RuleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class RetrogenHandler {

    public static void processChunk(LevelChunk chunk) {
        if (((IProcessedChunk) chunk).reliableReplacer$hasBeenProcessed()) {
            return;
        }

        Level level = chunk.getLevel();
        LevelChunkSection[] sections = chunk.getSections();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;

            int bottomY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            int startX = SectionPos.sectionToBlockCoord(chunk.getPos().x);
            int startZ = SectionPos.sectionToBlockCoord(chunk.getPos().z);

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = new BlockPos(startX + x, bottomY + y, startZ + z);
                        BlockState original = section.getBlockState(x, y, z);

                        BlockState replacement = RuleManager.getReplacement(original, pos, level, true);

                        if (replacement != original) {
                            level.setBlock(pos, replacement, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        }
                    }
                }
            }
        }

        ((IProcessedChunk) chunk).reliableReplacer$markProcessed();
    }
}