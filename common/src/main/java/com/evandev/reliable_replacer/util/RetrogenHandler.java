package com.evandev.reliable_replacer.util;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.config.RuleManager;
import com.evandev.reliable_replacer.data.ReplacementResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.storage.LevelData;

public class RetrogenHandler {

    public static void processChunk(LevelChunk chunk) {
        IProcessedChunk access = (IProcessedChunk) chunk;
        if (!ModConfig.get().enabled || (access.reliableReplacer$hasBeenProcessed() && !access.reliableReplacer$isDirty())) {
            return;
        }

        Level level = chunk.getLevel();
        boolean changed = false;
        LevelChunkSection[] sections = chunk.getSections();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        int chunkStartX = chunk.getPos().getMinBlockX();
        int chunkStartZ = chunk.getPos().getMinBlockZ();

        LevelData levelData = level.getLevelData();
        BlockPos spawnPos = new BlockPos(levelData.getXSpawn(), levelData.getYSpawn(), levelData.getZSpawn());

        RuleManager.RuleContext ctx = new RuleManager.RuleContext(level, mutablePos, spawnPos, true, null);

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;

            int bottomY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState original = section.getBlockState(x, y, z);
                        if (original.isAir()) continue;

                        mutablePos.set(chunkStartX + x, bottomY + y, chunkStartZ + z);

                        ctx.set(mutablePos, null);
                        ReplacementResult result = RuleManager.getReplacementResult(original, ctx, null, false);

                        if (result != null) {
                            BlockState replacement = result.state();
                            if (replacement != original) {
                                manageBlockEntity(chunk, mutablePos, result, level, replacement);
                                changed = true;
                            }
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

    private static void manageBlockEntity(LevelChunk chunk, BlockPos pos, ReplacementResult result, Level level, BlockState replacement) {
        CompoundTag nbtData = null;
        if (result.keepNbt()) {
            BlockEntity be = chunk.getBlockEntity(pos);
            if (be != null) {
                nbtData = be.saveWithoutMetadata();
                chunk.removeBlockEntity(pos);
            }
        }

        level.setBlock(pos, replacement, 2);

        if (nbtData != null) {
            BlockEntity newBe = chunk.getBlockEntity(pos);
            if (newBe != null) {
                newBe.load(nbtData);
            }
        }
    }
}