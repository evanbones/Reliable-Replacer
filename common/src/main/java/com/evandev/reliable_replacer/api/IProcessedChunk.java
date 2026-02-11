package com.evandev.reliable_replacer.api;

public interface IProcessedChunk {
    boolean reliableReplacer$hasBeenProcessed();
    void reliableReplacer$markProcessed();

    boolean reliableReplacer$isDirty();
    void reliableReplacer$setDirty(boolean dirty);
}