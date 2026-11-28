package com.voxelcore.client.handler;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.util.math.ChunkPos;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Diagnostic-only client chunk counter. It never changes Minecraft's chunk loading pipeline. */
public final class ChunkTracker {
    private static final Set<Long> LOADED_CHUNKS =
            Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

    private ChunkTracker() { }

    public static void onLoad(Chunk chunk) {
        if (chunk == null || chunk.getWorld() == null) return;
        ChunkPos pos = chunk.getPos();
        LOADED_CHUNKS.add(Long.valueOf(ChunkPos.asLong(pos.x, pos.z)));
    }

    public static void onUnload(Chunk chunk) {
        if (chunk == null || chunk.getWorld() == null) return;
        ChunkPos pos = chunk.getPos();
        LOADED_CHUNKS.remove(Long.valueOf(ChunkPos.asLong(pos.x, pos.z)));
    }

    public static int getLoadedCount() {
        return LOADED_CHUNKS.size();
    }

    public static void clear() {
        LOADED_CHUNKS.clear();
    }
}
