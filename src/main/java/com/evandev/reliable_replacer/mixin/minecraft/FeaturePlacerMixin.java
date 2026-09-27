package com.evandev.reliable_replacer.mixin.minecraft;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;
//? if >=26.3 {
/*import com.evandev.reliable_replacer.logic.RuleManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}

@IfMinecraftVersion(minVersion = "26.3")
@Mixin(targets = "net.minecraft.world.level.levelgen.placement.FeaturePlacer")
public class FeaturePlacerMixin {

    //? if >=26.3 {
    /*@Shadow
    @Final
    private WorldGenLevel level;

    @Inject(
            method = "place(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At("HEAD")
    )
    private void reliableReplacer$onPlacedFeatureStart(PlacedFeature placedFeature, RandomSource random, BlockPos origin, boolean biomeCheck, CallbackInfoReturnable<Boolean> cir) {
        RuleManager.pushActivePlacedFeature(placedFeature, this.level, origin);
    }

    @Inject(
            method = "place(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At("RETURN")
    )
    private void reliableReplacer$onPlacedFeatureEnd(PlacedFeature placedFeature, RandomSource random, BlockPos origin, boolean biomeCheck, CallbackInfoReturnable<Boolean> cir) {
        RuleManager.popActiveFeature();
    }

    @WrapOperation(
            method = "place(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/feature/Feature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean reliableReplacer$onFeaturePlace(Feature feature, WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos, Operation<Boolean> original) {
        RuleManager.pushActiveFeature(feature, level, pos);
        try {
            return original.call(feature, level, generator, random, pos);
        } finally {
            RuleManager.popActiveFeature();
        }
    }
    *///?}
}
