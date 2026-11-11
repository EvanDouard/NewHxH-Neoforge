package com.evandouard.hxh.client.look;

import com.evandouard.hxh.HxHMod;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Apparences des personnages, lues à la demande dans {@code assets/hxh/looks/<id>.json} et oubliées au F3+T. */
public final class CharacterLooks {
    private static final Map<String, Optional<CharacterLook>> CACHE = new ConcurrentHashMap<>();
    private static final java.util.List<Runnable> ON_RELOAD = new java.util.concurrent.CopyOnWriteArrayList<>();

    private CharacterLooks() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterClientReloadListenersEvent.class, e -> e.registerReloadListener(
                (ResourceManagerReloadListener) manager -> {
                    CACHE.clear();
                    ON_RELOAD.forEach(Runnable::run);
                }));
    }

    /** Pour les caches qui dépendent des ressources (skins trouvés ou non). */
    public static void onReload(Runnable action) {
        ON_RELOAD.add(action);
    }

    @Nullable
    public static CharacterLook get(String id) {
        if (id == null || id.isEmpty()) return null;
        return CACHE.computeIfAbsent(id, CharacterLooks::load).orElse(null);
    }

    private static Optional<CharacterLook> load(String id) {
        ResourceLocation loc = HxHMod.id("looks/" + id + ".json");
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(loc);
        if (res.isEmpty()) return Optional.empty();
        try (Reader reader = res.get().openAsReader()) {
            return Optional.of(CharacterLook.parse(JsonParser.parseReader(reader).getAsJsonObject()));
        } catch (Exception e) {
            HxHMod.LOGGER.error("[HxH] Apparence illisible {} : {}", loc, e.toString());
            return Optional.empty();
        }
    }
}
