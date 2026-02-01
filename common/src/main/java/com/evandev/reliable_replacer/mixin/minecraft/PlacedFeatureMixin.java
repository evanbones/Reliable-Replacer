package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.config.RuleManager;
import com.evandev.reliable_replacer.util.FeatureContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
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

        PlacedFeature self = (PlacedFeature) (Object) this;
        var registries = context.getLevel().registryAccess();
        ResourceLocation placedId = registries.registryOrThrow(Registries.PLACED_FEATURE).getKey(self);

        if (placedId != null) {
            if (RuleManager.shouldCancelFeature(placedId)) {
                cir.setReturnValue(false);
                return;
            }
            FeatureContext.push(placedId);
        }

        ResourceLocation configuredId = registries.registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getKey(self.feature().value());
        if (configuredId != null) {
            FeatureContext.push(configuredId);
        }
    }

    @Inject(method = "placeWithContext", at = @At("RETURN"))
    private void reliableReplacer$onPlaceWithContextReturn(PlacementContext context, RandomSource source, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.get().enabled) return;

        PlacedFeature self = (PlacedFeature) (Object) this;
        var registries = context.getLevel().registryAccess();

        ResourceLocation configuredId = registries.registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getKey(self.feature().value());
        if (configuredId != null) {
            FeatureContext.pop();
        }

        ResourceLocation placedId = registries.registryOrThrow(Registries.PLACED_FEATURE).getKey(self);
        if (placedId != null) {
            FeatureContext.pop();
        }
    }
}