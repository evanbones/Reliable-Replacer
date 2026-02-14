package com.evandev.reliable_replacer.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public record ReplacementResult(BlockState state, boolean keepNbt, Map<BlockPos, BlockState> additionalBlocks) {
}