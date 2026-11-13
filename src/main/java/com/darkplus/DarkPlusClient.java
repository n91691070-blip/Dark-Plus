package com.darkplus;

import com.darkplus.config.DarkPlusConfig;
import com.darkplus.core.AsyncComputePool;
import com.darkplus.core.MemoryOptimizer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarkPlusClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Dark+");
    private static long lastTickTimeNs = System.nanoTime();
    private static volatile boolean heavyLoadDetected = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Dark+ Universal Optimizer (1.21+)...");
        DarkPlusConfig.load();
        AsyncComputePool.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!DarkPlusConfig.enabled) return;

            long now = System.nanoTime();
            long elapsedMs = (now - lastTickTimeNs) / 1_000_000;
            lastTickTimeNs = now;

            // Detect frame-time or tick lag spikes (> 42ms)
            heavyLoadDetected = elapsedMs > 42;

            if (client.world != null && client.player != null && DarkPlusConfig.asyncEntityCulling) {
                AsyncComputePool.updateCameraState(client.player.getX(), client.player.getY(), client.player.getZ());
            }

            if (DarkPlusConfig.smartMemoryCleaner) {
                MemoryOptimizer.onClientTick();
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            AsyncComputePool.clearCaches();
            if (DarkPlusConfig.smartMemoryCleaner) {
                MemoryOptimizer.triggerSafeCleanup();
            }
        });
    }

    public static boolean isUnderHeavyLoad() {
        return heavyLoadDetected;
    }
}
