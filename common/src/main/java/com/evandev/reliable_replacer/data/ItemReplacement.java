package com.evandev.reliable_replacer.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.nbt.CompoundTag;

public class ItemReplacement {
    public String list_name = "Items";
    public String match_id;
    public String replace_id;
    @SerializedName("replace_nbt")
    public String replaceNbt;

    public transient CompoundTag parsedReplaceNbt;
}
