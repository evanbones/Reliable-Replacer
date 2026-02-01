package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.config.RuleManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(OreFeature.class)
public class OreFeatureMixin {

    @WrapOperation(
            method = "doPlace",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"
            )
    )
    private BlockState reliableReplacer$wrapOrePlacement(
            LevelChunkSection instance, int x, int y, int z, BlockState state, boolean useLocks,
            Operation<BlockState> original,
            WorldGenLevel level,
            net.minecraft.util.RandomSource random,
            net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration config,
            double minX, double maxX, double minZ, double maxZ, double minY, double maxY,
            int originX, int originY, int originZ
    ) {
        if (!ModConfig.get().enabled) {
            return original.call(instance, x, y, z, state, useLocks);
        }

        BlockPos pos = new BlockPos(originX + x, originY + y, originZ + z);
        BlockState replacement = RuleManager.getReplacement(state, pos, level, false, false);
        return original.call(instance, x, y, z, replacement, useLocks);
    }
}