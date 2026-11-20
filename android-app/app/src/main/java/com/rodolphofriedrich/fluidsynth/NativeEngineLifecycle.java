package com.rodolphofriedrich.fluidsynth;

/**
 * Coordinates ownership of the process-wide native FluidSynth instance.
 *
 * An Activity may be recreated while its previous audio thread is still
 * shutting down. Reference counting prevents the previous instance from
 * destroying the engine while a replacement Activity is already using it.
 */
public final class NativeEngineLifecycle {
    public interface Bridge {
        boolean initialize();
        void shutdown();
    }

    private static final Object LOCK = new Object();
    private static int activeClients;
    private static boolean engineReady;

    private NativeEngineLifecycle() {}

    public static boolean acquire(Bridge bridge) {
        if (bridge == null) throw new IllegalArgumentException("Bridge nula");

        synchronized (LOCK) {
            if (activeClients == 0) {
                engineReady = bridge.initialize();
                if (!engineReady) return false;
            } else if (!engineReady) {
                return false;
            }

            activeClients++;
            return true;
        }
    }

    public static void release(Bridge bridge) {
        if (bridge == null) return;

        synchronized (LOCK) {
            if (activeClients <= 0) return;

            activeClients--;
            if (activeClients == 0 && engineReady) {
                // Mark unavailable before shutdown so a later acquire waits
                // for this synchronized operation and then initializes anew.
                engineReady = false;
                bridge.shutdown();
            }
        }
    }
}
