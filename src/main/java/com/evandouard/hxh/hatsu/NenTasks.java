package com.evandouard.hxh.hatsu;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Petites actions différées des techniques (pluie de dragons, bombe à retardement, rafales…), jouées par le tick
 * serveur. Côté serveur seulement ; perdues à l'arrêt du serveur (ce sont des effets de quelques secondes).
 */
public final class NenTasks {
    private NenTasks() {}

    private record Task(long at, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static long now;

    /** Joue {@code action} dans {@code delayTicks} ticks (0 = au prochain tick). */
    public static void later(int delayTicks, Runnable action) {
        TASKS.add(new Task(now + Math.max(1, delayTicks), action));
    }

    public static void tick(MinecraftServer server) {
        now++;
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        TASKS.removeIf(t -> {
            if (t.at() > now) return false;
            due.add(t);
            return true;
        });
        for (Task t : due) {
            try {
                t.action().run();
            } catch (RuntimeException e) {
                com.evandouard.hxh.HxHMod.LOGGER.warn("[HxH] Action différée ignorée", e);
            }
        }
    }

    /** Tests en jeu : joue tout de suite ce qui est en attente. */
    public static void flushForTests() {
        now += 100000;
        tick(null);
    }
}
