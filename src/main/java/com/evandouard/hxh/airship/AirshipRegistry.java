package com.evandouard.hxh.airship;

import com.evandouard.hxh.HxHMod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/** Charge les destinations du dirigeable ({@code data/hxh/airship_destinations/*.json}) ; rechargées par /reload. */
public final class AirshipRegistry extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, AirshipDestination> DESTINATIONS = Map.of();

    private AirshipRegistry() {
        super(new Gson(), "airship_destinations");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new AirshipRegistry());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, AirshipDestination> loaded = new TreeMap<>();
        files.forEach((id, json) -> {
            try {
                loaded.put(id, AirshipDestination.parse(id, json.getAsJsonObject()));
            } catch (Exception e) {
                HxHMod.LOGGER.error("Destination de dirigeable {} invalide : {}", id, e.getMessage());
            }
        });
        DESTINATIONS = Map.copyOf(loaded);
        HxHMod.LOGGER.info("{} destination(s) de dirigeable chargée(s)", loaded.size());
    }

    public static Collection<AirshipDestination> all() {
        return new TreeMap<>(DESTINATIONS).values();
    }

    @Nullable
    public static AirshipDestination get(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : DESTINATIONS.get(rl);
    }
}
