package com.evandev.reliable_replacer.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public record ReplacementResult(BlockState state, boolean keepNbt, CompoundTag customNbt, Map<BlockPos, BlockState> additionalBlocks, Map<BlockPos, CompoundTag> additionalNbt) {
}