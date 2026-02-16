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

    public static boolean swapBlockWithNbt(Level level, BlockPos pos, BlockState replacement, boolean keepNbt, CompoundTag customNbt, int flags) {
        CompoundTag nbtData = null;

        if (keepNbt) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                nbtData = be.saveWithoutMetadata();
            }
        }

        boolean success;
        BlockState currentState = level.getBlockState(pos);

        if (currentState.equals(replacement)) {
            success = true;
        } else {
            success = level.setBlock(pos, replacement, flags);
        }

        if (success && (nbtData != null || customNbt != null)) {
            BlockEntity newBlockEntity = level.getBlockEntity(pos);
            if (newBlockEntity != null) {
                CompoundTag finalNbt = newBlockEntity.saveWithoutMetadata();
                if (nbtData != null) {
                    nbtData.remove("id");
                    nbtData.remove("x");
                    nbtData.remove("y");
                    nbtData.remove("z");
                    finalNbt.merge(nbtData);
                }
                if (customNbt != null) {
                    CompoundTag customCopy = customNbt.copy();
                    customCopy.remove("id");
                    customCopy.remove("x");
                    customCopy.remove("y");
                    customCopy.remove("z");
                    finalNbt.merge(customCopy);
                }
                newBlockEntity.load(finalNbt);
                newBlockEntity.setChanged();

                level.sendBlockUpdated(pos, currentState, replacement, flags);
            }
        }

        return success;
    }

    public static void safeSetBlock(LevelAccessor level, ChunkAccess currentChunk, BlockPos pos, BlockState state, CompoundTag customNbt) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;

        BlockState currentState = currentChunk.getBlockState(pos);
        boolean stateChanged = !currentState.equals(state);

        if (cx == currentChunk.getPos().x && cz == currentChunk.getPos().z) {
            if (stateChanged) {
                currentChunk.setBlockState(pos, state, false);
            }
            if (customNbt != null) {
                CompoundTag copy = customNbt.copy();
                copy.putInt("x", pos.getX());
                copy.putInt("y", pos.getY());
                copy.putInt("z", pos.getZ());

                BlockEntity be = currentChunk.getBlockEntity(pos);
                if (be != null) {
                    CompoundTag finalNbt = be.saveWithoutMetadata();
                    copy.remove("id");
                    finalNbt.merge(copy);
                    be.load(finalNbt);
                } else {
                    currentChunk.setBlockEntityNbt(copy);
                }
            }
        } else {
            try {
                if (stateChanged) {
                    level.setBlock(pos, state, 2);
                }
                if (customNbt != null) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be != null) {
                        CompoundTag finalNbt = be.saveWithoutMetadata();
                        CompoundTag copy = customNbt.copy();
                        copy.remove("id");
                        copy.remove("x");
                        copy.remove("y");
                        copy.remove("z");
                        finalNbt.merge(copy);
                        be.load(finalNbt);
                    }
                }
            } catch (Exception ignored) {
                // Ignore if offset is pushed out to unloaded chunks during generation bounds
            }
        }
    }
}