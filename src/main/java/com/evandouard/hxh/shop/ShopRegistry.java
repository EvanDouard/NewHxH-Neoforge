package com.evandouard.hxh.shop;

import com.evandouard.hxh.HxHMod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/** Charge les boutiques ({@code shops}) ; rechargées par /reload. */
public final class ShopRegistry extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, Shop> SHOPS = Map.of();

    private ShopRegistry() {
        super(new Gson(), "shops");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new ShopRegistry());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Shop> loaded = new TreeMap<>();
        files.forEach((id, json) -> {
            try {
                loaded.put(id, Shop.parse(id, json.getAsJsonObject()));
            } catch (Exception e) {
                HxHMod.LOGGER.error("Boutique {} invalide : {}", id, e.getMessage());
            }
        });
        SHOPS = Map.copyOf(loaded);
        HxHMod.LOGGER.info("{} boutique(s) chargée(s)", loaded.size());
    }

    @Nullable
    public static Shop get(ResourceLocation id) {
        return SHOPS.get(id);
    }

    /** Boutique tenue par ce PNJ, ou null. */
    @Nullable
    public static Shop forNpc(String npcId) {
        for (Shop s : SHOPS.values()) if (s.npc().equals(npcId)) return s;
        return null;
    }
}
