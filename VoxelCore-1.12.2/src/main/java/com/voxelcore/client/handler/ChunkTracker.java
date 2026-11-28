package com.voxelcore.client.handler;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.util.math.ChunkPos;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Diagnostic-only client chunk counter. Chunk coordinates are scoped to their
 * dimension because (x,z) pairs repeat in the Overworld, Nether and End.
 */
public final class ChunkTracker {
    private static final Set<ChunkKey> LOADED_CHUNKS =
            Collections.newSetFromMap(new ConcurrentHashMap<ChunkKey, Boolean>());

    private ChunkTracker() { }

    public static void onLoad(Chunk chunk) {
        if (chunk == null || chunk.getWorld() == null) return;
        LOADED_CHUNKS.add(keyFor(chunk));
    }

    public static void onUnload(Chunk chunk) {
        if (chunk == null || chunk.getWorld() == null) return;
        LOADED_CHUNKS.remove(keyFor(chunk));
    }

    private static ChunkKey keyFor(Chunk chunk) {
        World world = chunk.getWorld();
        ChunkPos pos = chunk.getPos();
        return new ChunkKey(world.provider.getDimension(), ChunkPos.asLong(pos.x, pos.z));
    }

    public static int getLoadedCount() {
        return LOADED_CHUNKS.size();
    }

    public static void clearDimension(int dimensionId) {
        for (ChunkKey key : LOADED_CHUNKS) {
            if (key.dimensionId == dimensionId) LOADED_CHUNKS.remove(key);
        }
    }

    public static void clear() {
        LOADED_CHUNKS.clear();
    }

    private static final class ChunkKey {
        final int dimensionId;
        final long chunkPosition;

        ChunkKey(int dimensionId, long chunkPosition) {
            this.dimensionId = dimensionId;
            this.chunkPosition = chunkPosition;
        }

        @Override
        public int hashCode() {
            int hash = (int) (chunkPosition ^ (chunkPosition >>> 32));
            return 31 * hash + dimensionId;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof ChunkKey)) return false;
            ChunkKey other = (ChunkKey) object;
            return dimensionId == other.dimensionId && chunkPosition == other.chunkPosition;
        }
    }
}
