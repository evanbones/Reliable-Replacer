package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.RuleManager;
import com.evandev.reliable_replacer.util.FeatureContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlacedFeature.class)
public class PlacedFeatureMixin {

    @WrapOperation(
            method = "place",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;placeWithContext(Lnet/minecraft/world/level/levelgen/placement/PlacementContext;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean wrapPlace(PlacedFeature instance, PlacementContext context, RandomSource random, BlockPos pos, Operation<Boolean> original) {
        WorldGenLevel level = context.getLevel();

        ResourceLocation id = level.registryAccess()
                .registryOrThrow(Registries.PLACED_FEATURE)
                .getKey(instance);

        if (id != null) {
            if (RuleManager.shouldCancelFeature(id, level)) {
                return false;
            }
            FeatureContext.push(id);
        }

        try {
            return original.call(instance, context, random, pos);
        } finally {
            if (id != null) {
                FeatureContext.pop();
            }
        }
    }
}