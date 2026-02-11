package com.evandev.reliable_replacer.mixin.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureUtils.class)
public class StructureUtilsMixin {

    @Inject(method = "getStructureTemplate", at = @At("HEAD"), cancellable = true)
    private static void reliableReplacer$bypassEmptyStructure(String name, ServerLevel level, CallbackInfoReturnable<StructureTemplate> cir) {
        if ("minecraft:empty_3x3x3".equals(name)) {
            StructureTemplate template = new StructureTemplate();
            CompoundTag tag = new CompoundTag();

            ListTag size = new ListTag();
            size.add(IntTag.valueOf(3));
            size.add(IntTag.valueOf(3));
            size.add(IntTag.valueOf(3));
            tag.put("size", size);

            tag.put("entities", new ListTag());
            tag.put("blocks", new ListTag());
            tag.put("palette", new ListTag());

            template.load(BuiltInRegistries.BLOCK.asLookup(), tag);

            cir.setReturnValue(template);
        }
    }
}