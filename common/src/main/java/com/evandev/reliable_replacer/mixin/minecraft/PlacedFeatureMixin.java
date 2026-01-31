package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.config.RuleManager;
import com.evandev.reliable_replacer.util.FeatureContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlacedFeature.class)
public class PlacedFeatureMixin {

    @Inject(method = "placeWithContext", at = @At("HEAD"), cancellable = true)
    private void reliableReplacer$onPlaceWithContextHead(PlacementContext context, RandomSource source, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.get().enabled) return;

        WorldGenLevel level = context.getLevel();
        Registry<PlacedFeature> placedRegistry = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);

        PlacedFeature self = (PlacedFeature) (Object) this;
        ResourceLocation placedId = placedRegistry.getKey(self);

        if (placedId != null && RuleManager.shouldCancelFeature(placedId, level)) {
            cir.setReturnValue(false);
            return;
        }

        if (placedId != null) {
            FeatureContext.push(placedId);
        }

        Registry<ConfiguredFeature<?, ?>> configuredRegistry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        ResourceLocation configuredId = configuredRegistry.getKey(self.feature().value());

        if (configuredId != null) {
            FeatureContext.push(configuredId);
        }
    }

    @Inject(method = "placeWithContext", at = @At("RETURN"))
    private void reliableReplacer$onPlaceWithContextReturn(PlacementContext context, RandomSource source, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.get().enabled) return;

        WorldGenLevel level = context.getLevel();
        Registry<PlacedFeature> placedRegistry = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        Registry<ConfiguredFeature<?, ?>> configuredRegistry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);

        PlacedFeature self = (PlacedFeature) (Object) this;

        if (configuredRegistry.getKey(self.feature().value()) != null) {
            FeatureContext.pop();
        }

        if (placedRegistry.getKey(self) != null) {
            FeatureContext.pop();
        }
    }
}