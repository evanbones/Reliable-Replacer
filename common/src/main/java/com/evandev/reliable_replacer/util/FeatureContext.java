package com.evandev.reliable_replacer.util;

import net.minecraft.resources.ResourceLocation;

public class FeatureContext {
    private static final ThreadLocal<ResourceLocation> CURRENT_FEATURE = new ThreadLocal<>();

    public static void push(ResourceLocation featureId) {
        CURRENT_FEATURE.set(featureId);
    }

    public static void pop() {
        CURRENT_FEATURE.remove();
    }

    public static ResourceLocation getCurrentFeature() {
        return CURRENT_FEATURE.get();
    }
}