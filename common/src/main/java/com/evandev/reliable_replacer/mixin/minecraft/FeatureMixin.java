package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.logic.RuleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Feature.class)
public class FeatureMixin {

    @Inject(
            method = "place(Lnet/minecraft/world/level/levelgen/feature/configurations/FeatureConfiguration;Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD")
    )
    private void reliableReplacer$onFeaturePlaceStart(FeatureConfiguration config, WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin, CallbackInfoReturnable<Boolean> cir) {
        int cx = origin.getX() >> 4;
        int cz = origin.getZ() >> 4;

        if (level.hasChunk(cx, cz)) {
            try {
                ChunkAccess chunk = level.getChunk(cx, cz);

                int qX = origin.getX() >> 2;
                int qY = origin.getY() >> 2;
                int qZ = origin.getZ() >> 2;

                var biomeOpt = chunk.getNoiseBiome(qX, qY, qZ).unwrapKey();
                biomeOpt.ifPresent(biomeResourceKey -> RuleManager.ACTIVE_FEATURE_BIOME.set(biomeResourceKey.location()));
            } catch (Exception ignored) {
            }
        }
    }

    @Inject(
            method = "place(Lnet/minecraft/world/level/levelgen/feature/configurations/FeatureConfiguration;Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("RETURN")
    )
    private void reliableReplacer$onFeaturePlaceEnd(FeatureConfiguration config, WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin, CallbackInfoReturnable<Boolean> cir) {
        RuleManager.ACTIVE_FEATURE_BIOME.remove();
    }
}