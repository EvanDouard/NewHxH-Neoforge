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

/** Charge les tranches d'étages ({@code arena_brackets}), triées par étage ; rechargées par /reload. */
public final class ArenaBrackets extends SimpleJsonResourceReloadListener {
    private static volatile List<ArenaBracket> BRACKETS = List.of(ArenaBracket.DEFAULT);

    private ArenaBrackets() {
        super(new Gson(), "arena_brackets");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new ArenaBrackets());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<ArenaBracket> list = new ArrayList<>();
        files.forEach((id, json) -> {
            try {
                list.add(ArenaBracket.parse(id, json));
            } catch (RuntimeException e) {
                HxHMod.LOGGER.error("[HxH] Tranche d'étages invalide {}", id, e);
            }
        });
        list.sort(Comparator.comparingInt(ArenaBracket::from));
        List<ArenaBracket> indexed = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) indexed.add(list.get(i).withIndex(i));
        BRACKETS = indexed.isEmpty() ? List.of(ArenaBracket.DEFAULT) : List.copyOf(indexed);
        HxHMod.LOGGER.info("[HxH] {} tranche(s) d'étages chargée(s)", indexed.size());
    }

    public static List<ArenaBracket> all() {
        return BRACKETS;
    }

    public static int count() {
        return BRACKETS.size();
    }

    public static ArenaBracket get(int index) {
        List<ArenaBracket> l = BRACKETS;
        return l.get(Math.max(0, Math.min(l.size() - 1, index)));
    }

    /** Tranche d'un étage (la dernière qui commence à cet étage ou en dessous). */
    public static ArenaBracket forFloor(int floor) {
        ArenaBracket found = BRACKETS.get(0);
        for (ArenaBracket b : BRACKETS) if (b.from() <= floor) found = b;
        return found;
    }

    public static int indexOf(int floor) {
        return forFloor(floor).index();
    }
}
