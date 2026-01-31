package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.RuleManager;
import com.evandev.reliable_replacer.util.FeatureContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlacedFeature.class)
public class PlacedFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void reliableReplacer$onPlace(
            WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos, CallbackInfoReturnable<Boolean> cir
    ) {
        ResourceLocation id = level.registryAccess()
                .registryOrThrow(Registries.PLACED_FEATURE)
                .getKey((PlacedFeature) (Object) this);

        if (id != null) {
            if (RuleManager.shouldCancelFeature(id, level)) {
                cir.setReturnValue(false);
                return;
            }

            FeatureContext.push(id);
        }
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void reliableReplacer$afterPlace(
            WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos, CallbackInfoReturnable<Boolean> cir
    ) {
        FeatureContext.pop();
    }
}