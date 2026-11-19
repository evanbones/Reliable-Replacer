package com.evandev.reliable_replacer.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public final class CompatUtil {
    public static void updatePoi(LevelAccessor level, BlockPos pos, BlockState oldState, BlockState newState) {
        ServerLevel serverLevel;
        if (level instanceof ServerLevel sl)
            serverLevel = sl;
        else if (level instanceof WorldGenRegion wgr)
            serverLevel = wgr.getLevel();
        else
            return;
        serverLevel.onBlockStateChange(pos, oldState, newState);
    }
}
