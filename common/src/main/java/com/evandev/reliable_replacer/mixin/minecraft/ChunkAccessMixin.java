package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.api.IProcessedChunk;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkAccess.class)
public class ChunkAccessMixin implements IProcessedChunk {

    @Unique
    private boolean reliableReplacer$processed = false;

    @Unique
    private boolean reliableReplacer$dirty = false;

    @Unique
    private int reliableReplacer$rulesHash = 0;

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

    @Override
    public int reliableReplacer$getRulesHash() {
        return reliableReplacer$rulesHash;
    }

    @Override
    public void reliableReplacer$setRulesHash(int hash) {
        this.reliableReplacer$rulesHash = hash;
    }
}