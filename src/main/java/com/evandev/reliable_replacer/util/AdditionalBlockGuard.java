package com.evandev.reliable_replacer.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

public class AdditionalBlockGuard {
    private final Map<BlockPos, Block> guarded = new HashMap<>();
    private long stamp = Long.MIN_VALUE;

    public void guard(BlockPos pos, Block inputBlock, long stamp) {
        if (stamp != this.stamp) {
            guarded.clear();
            this.stamp = stamp;
        }
        guarded.put(pos.immutable(), inputBlock);
    }

    public boolean shouldSkip(BlockPos pos, Block incoming, long stamp) {
        if (guarded.isEmpty() || stamp != this.stamp) return false;
        if (guarded.get(pos) != incoming) return false;
        guarded.remove(pos);
        return true;
    }
}
