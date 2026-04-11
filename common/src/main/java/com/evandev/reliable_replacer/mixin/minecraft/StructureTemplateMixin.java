package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Objects;

@Mixin(StructureTemplate.class)
public class StructureTemplateMixin {

    @Inject(method = "load", at = @At("HEAD"))
    private void reliableReplacer$remapStructurePalette(HolderGetter<Block> blockGetter, CompoundTag tag, CallbackInfo ci) {
        Map<String, String> remapper = ModConfig.get().missingIdMap;
        if (remapper == null || remapper.isEmpty()) return;

        if (tag.contains("palette")) {
            ListTag palette = tag.getListOrEmpty("palette");
            for (int i = 0; i < palette.size(); i++) {
                palette.getCompound(i).ifPresent(entry -> {
                    String name = entry.getStringOr("Name", "");
                    if (!name.isEmpty() && remapper.containsKey(name)) {
                        entry.putString("Name", remapper.get(name));
                    }
                });
            }
        }

        if (tag.contains("palettes")) {
            ListTag palettes = tag.getListOrEmpty("palettes");
            for (int i = 0; i < palettes.size(); i++) {
                palettes.getList(i).ifPresent(palette -> {
                    for (int j = 0; j < palette.size(); j++) {
                        palette.getCompound(j).ifPresent(entry -> {
                            String name = entry.getStringOr("Name", "");
                            if (!name.isEmpty() && remapper.containsKey(name)) {
                                entry.putString("Name", remapper.get(name));
                            }
                        });
                    }
                });
            }
        }
    }

    @WrapOperation(
            method = "placeInWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/world/level/storage/ValueInput;)V"
            )
    )
    private void reliableReplacer$fixNbtId(BlockEntity instance, ValueInput input, Operation<Void> original, @Local(name = "blockInfo") StructureTemplate.StructureBlockInfo blockInfo, @Local(name = "level") ServerLevelAccessor level) {
        if (instance != null && blockInfo.nbt() != null) {
            String worldTileId = Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(instance.getType())).toString();
            String nbtTileId = blockInfo.nbt().getStringOr("id", "");

            if (!worldTileId.equals(nbtTileId)) {
                CompoundTag newTag = blockInfo.nbt().copy();
                newTag.putString("id", worldTileId);

                try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(com.evandev.reliable_replacer.Constants.LOG)) {
                    input = TagValueInput.create(reporter, level.registryAccess(), newTag);
                }
            }
        }
        original.call(instance, input);
    }
}