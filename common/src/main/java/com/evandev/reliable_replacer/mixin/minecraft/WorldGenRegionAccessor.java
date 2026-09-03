package com.evandev.reliable_replacer.mixin.minecraft;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldGenRegion.class)
public interface WorldGenRegionAccessor {
    @Accessor("writeRadiusCutoff")
    int reliableReplacer$getWriteRadiusCutoff();

    @Accessor("center")
    ChunkAccess reliableReplacer$getCenterChunk();
}
