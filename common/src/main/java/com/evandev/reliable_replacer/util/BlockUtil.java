package com.evandev.reliable_replacer.util;

import com.evandev.reliable_replacer.data.ItemReplacement;
import com.evandev.reliable_replacer.logic.RuleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public class BlockUtil {

    public static void processChunkBlocks(ChunkAccess chunk, BiConsumer<BlockPos, BlockState> action) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int chunkStartX = chunk.getPos().getMinBlockX();
        int chunkStartZ = chunk.getPos().getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();
        boolean processAir = RuleManager.HAS_AIR_RULES;

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];

            if (section == null || (section.hasOnlyAir() && !processAir)) continue;
            int bottomY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState original = section.getBlockState(x, y, z);
                        if (original.isAir() && !processAir) continue;

                        mutablePos.set(chunkStartX + x, bottomY + y, chunkStartZ + z);
                        action.accept(mutablePos, original);
                    }
                }
            }
        }
    }

    public static void applyItemReplacements(CompoundTag blockEntityNbt, List<ItemReplacement> replacements) {
        if (replacements == null || replacements.isEmpty() || blockEntityNbt == null) return;

        for (ItemReplacement replacement : replacements) {
            String[] path = replacement.list_name.split("\\.");
            CompoundTag currentTag = blockEntityNbt;

            for (int i = 0; i < path.length - 1; i++) {
                if (currentTag.contains(path[i], Tag.TAG_COMPOUND)) {
                    currentTag = currentTag.getCompound(path[i]);
                } else {
                    currentTag = null;
                    break;
                }
            }

            if (currentTag == null) continue;
            String targetKey = path[path.length - 1];

            if (currentTag.contains(targetKey, Tag.TAG_LIST)) {
                ListTag listTag = currentTag.getList(targetKey, Tag.TAG_COMPOUND);
                for (int i = 0; i < listTag.size(); i++) {
                    CompoundTag itemTag = listTag.getCompound(i);
                    replaceItemInTag(itemTag, replacement);
                }
            } else if (currentTag.contains(targetKey, Tag.TAG_COMPOUND)) {
                CompoundTag itemTag = currentTag.getCompound(targetKey);
                replaceItemInTag(itemTag, replacement);
            }
        }
    }

    private static void replaceItemInTag(CompoundTag itemTag, ItemReplacement replacement) {
        if (itemTag.getString("id").matches(replacement.match_id.replace("*", ".*"))) {

            if (replacement.probability != null && ThreadLocalRandom.current().nextFloat() > replacement.probability) {
                return;
            }

            itemTag.putString("id", replacement.replace_id);

            if (replacement.parsedReplaceNbt != null) {
                if (!itemTag.contains("tag", Tag.TAG_COMPOUND)) {
                    itemTag.put("tag", new CompoundTag());
                }
                itemTag.getCompound("tag").merge(replacement.parsedReplaceNbt);
            }
        }
    }

    public static boolean swapBlockWithNbt(Level level, BlockPos pos, BlockState replacement, boolean keepNbt, CompoundTag customNbt, List<ItemReplacement> itemReplacements, int flags) {
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

        if (success && (nbtData != null || customNbt != null || (itemReplacements != null && !itemReplacements.isEmpty()))) {
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

                applyItemReplacements(finalNbt, itemReplacements);

                newBlockEntity.load(finalNbt);
                newBlockEntity.setChanged();

                level.sendBlockUpdated(pos, currentState, replacement, flags);
            }
        }

        return success;
    }

    public static void safeSetBlock(LevelAccessor level, ChunkAccess currentChunk, BlockPos pos, BlockState state, CompoundTag customNbt, List<ItemReplacement> itemReplacements) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;

        BlockState currentState = currentChunk.getBlockState(pos);
        boolean stateChanged = !currentState.equals(state);

        if (cx == currentChunk.getPos().x && cz == currentChunk.getPos().z) {
            if (stateChanged) {
                currentChunk.setBlockState(pos, state, false);
            }
            if (customNbt != null || (itemReplacements != null && !itemReplacements.isEmpty())) {
                BlockEntity be = currentChunk.getBlockEntity(pos);
                CompoundTag finalNbt = new CompoundTag();

                if (be != null) {
                    finalNbt = be.saveWithoutMetadata();
                } else {
                    CompoundTag deferredNbt = currentChunk.getBlockEntityNbtForSaving(pos);
                    if (deferredNbt != null) {
                        finalNbt = deferredNbt.copy();
                    }
                }

                if (customNbt != null) {
                    CompoundTag copy = customNbt.copy();
                    copy.putInt("x", pos.getX());
                    copy.putInt("y", pos.getY());
                    copy.putInt("z", pos.getZ());
                    copy.remove("id");
                    finalNbt.merge(copy);
                }

                applyItemReplacements(finalNbt, itemReplacements);

                if (be != null) {
                    be.load(finalNbt);
                } else {
                    currentChunk.setBlockEntityNbt(finalNbt);
                }
            }
        } else {
            try {
                if (stateChanged) {
                    level.setBlock(pos, state, 50);
                }
                if (customNbt != null || (itemReplacements != null && !itemReplacements.isEmpty())) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be != null) {
                        CompoundTag finalNbt = be.saveWithoutMetadata();
                        if (customNbt != null) {
                            CompoundTag copy = customNbt.copy();
                            copy.remove("id");
                            copy.remove("x");
                            copy.remove("y");
                            copy.remove("z");
                            finalNbt.merge(copy);
                        }

                        applyItemReplacements(finalNbt, itemReplacements);
                        be.load(finalNbt);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}