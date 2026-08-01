package com.evandev.reliable_replacer.mixin.minecraft;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldGenRegion.class)
public interface WorldGenRegionAccessor {
    @Accessor("generatingStep")
    ChunkStep reliableReplacer$getGeneratingStep();

    @Accessor("center")
    ChunkAccess reliableReplacer$getCenterChunk();
}
