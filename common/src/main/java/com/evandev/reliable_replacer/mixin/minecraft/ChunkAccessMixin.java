package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.util.IProcessedChunk;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkAccess.class)
public class ChunkAccessMixin implements IProcessedChunk {

    @Unique
    private boolean reliableReplacer$processed = false;

    @Unique
    private boolean reliableReplacer$dirty = false;

    @Override
    public boolean reliableReplacer$hasBeenProcessed() {
        return reliableReplacer$processed;
    }

    @Override
    public void reliableReplacer$markProcessed() {
        this.reliableReplacer$processed = true;
    }

    @Override
    public boolean reliableReplacer$isDirty() {
        return reliableReplacer$dirty;
    }

    @Override
    public void reliableReplacer$setDirty(boolean dirty) {
        this.reliableReplacer$dirty = dirty;
    }
}