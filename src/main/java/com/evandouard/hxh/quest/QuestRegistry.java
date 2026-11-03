package com.evandouard.hxh.quest;

import com.evandouard.hxh.HxHMod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Charge les quêtes depuis les datapacks (dossier {@code quests}) et les recharge avec /reload.
 */
public final class QuestRegistry extends SimpleJsonResourceReloadListener {
    private static final Map<ResourceLocation, Quest> QUESTS = new HashMap<>();
    public static final ResourceLocation HUNTER_EXAM = HxHMod.id("hunter_exam");
    public static final ResourceLocation HEAVENS_ARENA = HxHMod.id("heavens_arena");

    private QuestRegistry() {
        super(new Gson(), "quests");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new QuestRegistry());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        QUESTS.clear();
        files.forEach((id, json) -> {
            try {
                QUESTS.put(id, Quest.parse(id, json));
            } catch (RuntimeException e) {
                HxHMod.LOGGER.error("[HxH] Quête invalide {}", id, e);
            }
        });
        HxHMod.LOGGER.info("[HxH] {} quête(s) chargée(s)", QUESTS.size());
    }

    @Nullable
    public static Quest get(ResourceLocation id) {
        return QUESTS.get(id);
    }
}
