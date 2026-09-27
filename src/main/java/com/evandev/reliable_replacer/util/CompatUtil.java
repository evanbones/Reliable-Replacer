package com.evandev.reliable_replacer.util;

import com.evandev.reliable_replacer.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.LevelData;
import org.jetbrains.annotations.Nullable;
//? if >=1.21.2 {
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
//?}

import java.util.Optional;

public final class CompatUtil {

    public static Identifier keyId(ResourceKey<?> key) {
        //? if <1.21.11 {
        /*return key.location();
        *///?} else {
        return key.identifier();
        //?}
    }

    public static Block getBlock(Identifier id) {
        //? if <1.21.2 {
        /*return BuiltInRegistries.BLOCK.get(id);
        *///?} else {
        return BuiltInRegistries.BLOCK.getValue(id);
        //?}
    }

    @Nullable
    public static <T> T getValue(Registry<T> registry, Identifier id) {
        //? if <1.21.2 {
        /*return registry.get(id);
        *///?} else {
        return registry.getValue(id);
        //?}
    }

    public static <T> Optional<Holder.Reference<T>> getHolder(Registry<T> registry, ResourceKey<T> key) {
        //? if <1.21.2 {
        /*return registry.getHolder(key);
        *///?} else {
        return registry.get(key);
        //?}
    }

    public static <T> Registry<T> registryOrThrow(RegistryAccess access, ResourceKey<? extends Registry<? extends T>> key) {
        //? if <1.21.2 {
        /*return access.registryOrThrow(key);
        *///?} else {
        return access.lookupOrThrow(key);
        //?}
    }

    public static <T> Optional<Registry<T>> registry(RegistryAccess access, ResourceKey<? extends Registry<? extends T>> key) {
        //? if <1.21.2 {
        /*return access.registry(key);
        *///?} else {
        return access.lookup(key);
        //?}
    }

    public static int chunkX(ChunkPos pos) {
        //? if <1.21.2 {
        /*return pos.x;
        *///?} else {
        return pos.x();
        //?}
    }

    public static int chunkZ(ChunkPos pos) {
        //? if <1.21.2 {
        /*return pos.z;
        *///?} else {
        return pos.z();
        //?}
    }

    public static ChunkPos chunkPos(BlockPos pos) {
        //? if <1.21.2 {
        /*return new ChunkPos(pos);
        *///?} else {
        return ChunkPos.containing(pos);
        //?}
    }

    public static ChunkPos unpackChunkPos(long packed) {
        return new ChunkPos(ChunkPos.getX(packed), ChunkPos.getZ(packed));
    }

    public static long packChunkPos(int x, int z) {
        //? if <1.21.2 {
        /*return ChunkPos.asLong(x, z);
        *///?} else {
        return ChunkPos.pack(x, z);
        //?}
    }

    public static BlockPos spawnPos(Level level) {
        //? if <1.21.2 {
        /*return level.getSharedSpawnPos();
        *///?} else {
        return level.getRespawnData().pos();
        //?}
    }

    public static BlockPos spawnPos(LevelData levelData) {
        //? if <1.21.2 {
        /*return new BlockPos(levelData.getSpawnPos());
        *///?} else {
        return levelData.getRespawnData().pos();
        //?}
    }

    public static CompoundTag parseNbt(String nbt) throws Exception {
        //? if <1.21.2 {
        /*return TagParser.parseTag(nbt);
        *///?} else {
        return TagParser.parseCompoundFully(nbt);
        //?}
    }

    public static void markUnsaved(ChunkAccess chunk) {
        //? if <1.21.2 {
        /*chunk.setUnsaved(true);
        *///?} else {
        chunk.markUnsaved();
        //?}
    }

    public static void loadBlockEntity(BlockEntity blockEntity, CompoundTag tag, HolderLookup.Provider registries) {
        //? if <1.21.2 {
        /*blockEntity.loadWithComponents(tag, registries);
        *///?} else {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Constants.LOG)) {
            blockEntity.loadCustomOnly(TagValueInput.create(reporter, registries, tag));
        }
        //?}
    }
}
