package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.api.IProcessedChunk;
import com.evandev.reliable_replacer.config.ModConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;

@Mixin(SerializableChunkData.class)
public class SerializableChunkDataMixin implements IProcessedChunk {

    @Unique
    private boolean reliableReplacer$processed = false;
    @Unique
    private boolean reliableReplacer$dirty = false;

    @Inject(method = "parse", at = @At("HEAD"))
    private static void onParseHead(LevelHeightAccessor levelHeight, PalettedContainerFactory containerFactory, CompoundTag chunkData, CallbackInfoReturnable<SerializableChunkData> cir) {
        Map<String, String> remapper = ModConfig.get().missingIdMap;
        if (remapper == null || remapper.isEmpty()) return;

        ListTag sections = chunkData.getListOrEmpty("sections");
        if (sections.isEmpty()) return;

        for (int i = 0; i < sections.size(); i++) {
            Optional<CompoundTag> optSection = sections.getCompound(i);
            if (optSection.isPresent()) {
                CompoundTag section = optSection.get();
                Optional<CompoundTag> optBlockStates = section.getCompound("block_states");

                if (optBlockStates.isPresent()) {
                    CompoundTag blockStates = optBlockStates.get();
                    ListTag palette = blockStates.getListOrEmpty("palette");

                    for (int j = 0; j < palette.size(); j++) {
                        Optional<CompoundTag> optEntry = palette.getCompound(j);
                        if (optEntry.isPresent()) {
                            CompoundTag entry = optEntry.get();
                            String name = entry.getStringOr("Name", "");

                            if (!name.isEmpty() && remapper.containsKey(name)) {
                                entry.putString("Name", remapper.get(name));
                            }
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "parse", at = @At("RETURN"))
    private static void onParseReturn(LevelHeightAccessor levelHeight, PalettedContainerFactory containerFactory, CompoundTag chunkData, CallbackInfoReturnable<SerializableChunkData> cir) {
        SerializableChunkData data = cir.getReturnValue();
        if (data != null) {
            boolean isProcessed = chunkData.getBooleanOr("ReliableReplacerProcessed", false);
            boolean isDirty = chunkData.getBooleanOr("ReliableReplacerDirty", false);

            IProcessedChunk dataAccess = (IProcessedChunk) (Object) data;

            if (isProcessed) {
                dataAccess.reliableReplacer$markProcessed();
            } else {
                dataAccess.reliableReplacer$resetProcessed();
            }
            dataAccess.reliableReplacer$setDirty(isDirty);
        }
    }

    @Inject(method = "copyOf", at = @At("RETURN"))
    private static void onCopyOf(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<SerializableChunkData> cir) {
        SerializableChunkData data = cir.getReturnValue();
        if (data != null && chunk instanceof IProcessedChunk processedChunk) {
            IProcessedChunk dataAccess = (IProcessedChunk) (Object) data;

            if (processedChunk.reliableReplacer$hasBeenProcessed()) {
                dataAccess.reliableReplacer$markProcessed();
            }
            dataAccess.reliableReplacer$setDirty(processedChunk.reliableReplacer$isDirty());
        }
    }

    @Override
    public boolean reliableReplacer$hasBeenProcessed() {
        return this.reliableReplacer$processed;
    }

    @Override
    public void reliableReplacer$markProcessed() {
        this.reliableReplacer$processed = true;
    }

    @Override
    public void reliableReplacer$resetProcessed() {
        this.reliableReplacer$processed = false;
    }

    @Override
    public boolean reliableReplacer$isDirty() {
        return this.reliableReplacer$dirty;
    }

    @Override
    public void reliableReplacer$setDirty(boolean dirty) {
        this.reliableReplacer$dirty = dirty;
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void onReadReturn(ServerLevel level, PoiManager poiManager, RegionStorageInfo regionInfo, ChunkPos pos, CallbackInfoReturnable<ProtoChunk> cir) {
        ChunkAccess chunk = cir.getReturnValue();
        if (chunk instanceof IProcessedChunk processedChunk) {
            if (this.reliableReplacer$processed) processedChunk.reliableReplacer$markProcessed();
            if (this.reliableReplacer$dirty) processedChunk.reliableReplacer$setDirty(true);
        }
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void onWrite(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        if (tag != null) {
            if (this.reliableReplacer$processed) tag.putBoolean("ReliableReplacerProcessed", true);
            if (this.reliableReplacer$dirty) tag.putBoolean("ReliableReplacerDirty", true);
        }
    }
}