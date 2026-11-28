package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.Locale;

@SideOnly(Side.CLIENT)
public class ClientEventHandler {
    private static final int SAMPLE_COUNT = 120;
    private final double[] frameSamplesMs = new double[SAMPLE_COUNT];
    private final double[] percentileScratch = new double[SAMPLE_COUNT];
    private long lastFrameNanos;
    private double averageFrameMs;
    private int sampleIndex;
    private int sampleSize;
    private int overlayRefreshCounter;
    private String[] cachedLines = new String[0];

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ParticleQualityController.update(Minecraft.getMinecraft());
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getWorld().isRemote) ChunkTracker.onLoad(event.getChunk());
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getWorld().isRemote) ChunkTracker.onUnload(event.getChunk());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            ParticleQualityController.restore(Minecraft.getMinecraft());
            ChunkTracker.clearDimension(event.getWorld().provider.getDimension());
            BlockSurfaceDetailRenderer.clearCache();
            LavaSurfaceRenderer.clearCache();
            WaterEffectsHandler.clearAll();
            resetFrameSamples();
            cachedLines = new String[0];
        }
    }

    /** Optional visual culling. Disabled by default and intentionally limited to non-player living entities. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingRender(RenderLivingEvent.Pre<EntityLivingBase> event) {
        if (!ModConfig.ENABLE_ENTITY_DISTANCE_CULLING || ModConfig.ENTITY_DISTANCE_PERCENT >= 100) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) return;

        EntityLivingBase entity = event.getEntity();
        if (entity == null || entity == mc.player || entity instanceof EntityPlayer || entity.isDead) return;
        if (entity.world != mc.world || entity.isBeingRidden()) return;

        double viewDistance = Math.max(16.0D, mc.gameSettings.renderDistanceChunks * 16.0D);
        double cutoff = viewDistance * (ModConfig.ENTITY_DISTANCE_PERCENT / 100.0D);
        if (entity.getDistanceSq(mc.player.posX, mc.player.posY, mc.player.posZ) > cutoff * cutoff) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (!ModConfig.SHOW_OVERLAY || event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) return;

        updateFrameTime();
        if (++overlayRefreshCounter >= 6 || cachedLines.length == 0) {
            overlayRefreshCounter = 0;
            cachedLines = createLines(mc);
        }

        FontRenderer font = mc.fontRenderer;
        int width = 0;
        for (String line : cachedLines) width = Math.max(width, font.getStringWidth(line));
        int boxWidth = width + 12;
        int boxHeight = cachedLines.length * 10 + 8;
        Gui.drawRect(6, 6, 6 + boxWidth, 6 + boxHeight, 0xA50B1018);

        int y = 10;
        for (String line : cachedLines) {
            font.drawStringWithShadow(line, 11, y, 0xE9F4FF);
            y += 10;
        }
    }

    private void updateFrameTime() {
        long now = System.nanoTime();
        if (lastFrameNanos != 0L) {
            double sample = (now - lastFrameNanos) / 1000000.0D;
            if (sample > 0.0D && sample < 1000.0D) {
                frameSamplesMs[sampleIndex] = sample;
                sampleIndex = (sampleIndex + 1) % SAMPLE_COUNT;
                sampleSize = Math.min(SAMPLE_COUNT, sampleSize + 1);
                averageFrameMs = averageFrameMs == 0.0D ? sample : averageFrameMs * 0.90D + sample * 0.10D;
            }
        }
        lastFrameNanos = now;
    }

    private void resetFrameSamples() {
        Arrays.fill(frameSamplesMs, 0.0D);
        lastFrameNanos = 0L;
        averageFrameMs = 0.0D;
        sampleIndex = 0;
        sampleSize = 0;
    }

    private double get95thPercentileFrameMs() {
        if (sampleSize == 0) return 0.0D;
        System.arraycopy(frameSamplesMs, 0, percentileScratch, 0, sampleSize);
        Arrays.sort(percentileScratch, 0, sampleSize);
        int index = Math.min(sampleSize - 1, (int) Math.ceil(sampleSize * 0.95D) - 1);
        return percentileScratch[index];
    }

    private String[] createLines(Minecraft mc) {
        String frame = averageFrameMs > 0.0D
                ? String.format(Locale.ROOT, "Frame: %.1f ms avg / %.1f ms p95", averageFrameMs, get95thPercentileFrameMs())
                : "Frame: measuring...";
        int entityCount = mc.world.loadedEntityList.size();
        int tileCount = mc.world.loadedTileEntityList.size();

        java.util.List<String> lines = new java.util.ArrayList<String>();
        lines.add("§bMinefade §7| §fRendimiento");
        lines.add("FPS: " + Minecraft.getDebugFPS() + "  |  " + frame);

        if (ModConfig.SHOW_MEMORY) {
            Runtime runtime = Runtime.getRuntime();
            long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
            long maxMb = runtime.maxMemory() / (1024L * 1024L);
            lines.add("Memory: " + usedMb + " / " + maxMb + " MB");
        }
        if (ModConfig.SHOW_CHUNK_COUNT) lines.add("Client chunks: " + ChunkTracker.getLoadedCount());
        if (ModConfig.SHOW_ENTITY_COUNT) lines.add("Entities: " + entityCount);
        if (ModConfig.SHOW_TILE_ENTITY_COUNT) lines.add("Tile entities: " + tileCount);
        lines.add("Water FX: " + (ModConfig.ENABLE_WATER_EFFECTS ? "ON" : "OFF")
                + "  |  Radius: " + ModConfig.WATER_EFFECT_RADIUS);
        lines.add("Dimension: " + mc.world.provider.getDimension());
        return lines.toArray(new String[lines.size()]);
    }
}
