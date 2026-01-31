package com.evandev.reliable_replacer.util;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Stream;

public class FeatureContext {
    private static final ThreadLocal<Deque<ResourceLocation>> FEATURE_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    public static void push(ResourceLocation featureId) {
        FEATURE_STACK.get().push(featureId);
    }

    public static void pop() {
        Deque<ResourceLocation> stack = FEATURE_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static ResourceLocation getCurrentFeature() {
        return FEATURE_STACK.get().peek();
    }

    public static Stream<ResourceLocation> getFeatureStack() {
        return FEATURE_STACK.get().stream();
    }
}