package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.util.FeatureContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ConfiguredFeature.class)
public class ConfiguredFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"))
    private void reliableReplacer$onPlaceHead(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Registry<ConfiguredFeature<?, ?>> registry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        ConfiguredFeature<?, ?> self = (ConfiguredFeature<?, ?>) (Object) this;

        ResourceLocation id = registry.getKey(self);
        if (id != null) {
            FeatureContext.push(id);
        }
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void reliableReplacer$onPlaceReturn(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Registry<ConfiguredFeature<?, ?>> registry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        ConfiguredFeature<?, ?> self = (ConfiguredFeature<?, ?>) (Object) this;

        if (registry.getKey(self) != null) {
            FeatureContext.pop();
        }
    }
}