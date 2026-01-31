package com.evandev.reliable_replacer.util;

public interface IProcessedChunk {
    boolean reliableReplacer$hasBeenProcessed();
    void reliableReplacer$markProcessed();
}