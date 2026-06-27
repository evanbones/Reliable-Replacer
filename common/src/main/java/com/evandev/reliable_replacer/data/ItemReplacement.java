package com.evandev.reliable_replacer.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class ItemReplacement {
    public String list_name = "Items";
    public String match_id;
    public List<String> match_ids = new ArrayList<>();
    public String replace_id;
    @SerializedName("replace_nbt")
    public String replaceNbt;
    @SerializedName("probability")
    public Float probability = null;

    public transient CompoundTag parsedReplaceNbt;
}