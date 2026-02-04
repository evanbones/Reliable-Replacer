package com.evandev.reliable_replacer.data;

import net.minecraft.world.level.block.state.BlockState;

public record ReplacementResult(BlockState state, boolean keepNbt) {
}