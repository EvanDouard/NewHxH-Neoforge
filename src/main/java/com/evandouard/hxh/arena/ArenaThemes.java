package com.evandouard.hxh.arena;

import com.evandouard.hxh.HxHMod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Charge les décors d'étages ({@code arena_themes}) ; rechargés par /reload. */
public final class ArenaThemes extends SimpleJsonResourceReloadListener {
    private static volatile List<ArenaTheme> THEMES = List.of();

    private ArenaThemes() {
        super(new Gson(), "arena_themes");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new ArenaThemes());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<ArenaTheme> list = new ArrayList<>();
        files.forEach((id, json) -> {
            try {
                list.add(ArenaTheme.parse(id, json));
            } catch (RuntimeException e) {
                HxHMod.LOGGER.error("[HxH] Décor d'étage invalide {}", id, e);
            }
        });
        // La tranche la plus étroite gagne quand deux décors se recouvrent
        list.sort(Comparator.comparingInt(t -> t.to() - t.from()));
        THEMES = List.copyOf(list);
        HxHMod.LOGGER.info("[HxH] {} décor(s) d'étage chargé(s)", list.size());
    }

    public static ArenaTheme forFloor(int floor) {
        for (ArenaTheme t : THEMES) if (t.covers(floor)) return t;
        return ArenaTheme.DEFAULT;
    }

    public static int count() {
        return THEMES.size();
    }
}
