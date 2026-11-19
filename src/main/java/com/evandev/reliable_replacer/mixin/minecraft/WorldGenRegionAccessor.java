package com.evandev.reliable_replacer.mixin.minecraft;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
//? if >=1.21
import net.minecraft.world.level.chunk.status.ChunkStep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldGenRegion.class)
public interface WorldGenRegionAccessor {
    //? if >=1.21 {
    @Accessor("generatingStep")
    ChunkStep reliableReplacer$getGeneratingStep();
    //?} else {
    /*@Accessor("writeRadiusCutoff")
    int reliableReplacer$getWriteRadiusCutoff();
    *///?}

    @Accessor("center")
    ChunkAccess reliableReplacer$getCenterChunk();
}
