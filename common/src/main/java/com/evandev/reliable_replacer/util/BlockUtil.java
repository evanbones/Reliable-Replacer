package com.evandev.reliable_replacer.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.function.BiConsumer;

public class BlockUtil {

    /**
     * Iterates over every non-air block in a chunk and performs an action.
     */
    public static void processChunkBlocks(ChunkAccess chunk, BiConsumer<BlockPos, BlockState> action) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int chunkStartX = chunk.getPos().getMinBlockX();
        int chunkStartZ = chunk.getPos().getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();

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
                        action.accept(mutablePos, original);
                    }
                }
            }
        }
    }

    /**
     * Replaces a block in the level, optionally preserving NBT data.
     */
    public static boolean swapBlockWithNbt(Level level, BlockPos pos, BlockState replacement, boolean keepNbt, int flags) {
        CompoundTag nbtData = null;

        if (keepNbt) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                nbtData = be.saveWithoutMetadata(level.registryAccess());
            }
        }

        boolean success = level.setBlock(pos, replacement, flags);

        if (success && nbtData != null) {
            BlockEntity newBlockEntity = level.getBlockEntity(pos);
            if (newBlockEntity != null) {
                newBlockEntity.loadWithComponents(nbtData, level.registryAccess());
            }
        }

        return success;
    }

    public static void safeSetBlock(LevelAccessor level, ChunkAccess currentChunk, BlockPos pos, BlockState state) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;

        // Prevents out-of-bounds cross-chunk writing deadlocks
        if (cx == currentChunk.getPos().x && cz == currentChunk.getPos().z) {
            currentChunk.setBlockState(pos, state, false);
        } else {
            try {
                level.setBlock(pos, state, 2);
            } catch (Exception ignored) {
                // Ignore if offset is pushed out to unloaded chunks during generation bounds
            }
        }
    }
}