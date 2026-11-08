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
        BROKEN.clear();
        files.forEach((id, json) -> {
            try {
                QUESTS.put(id, Quest.parse(id, json));
            } catch (RuntimeException e) {
                HxHMod.LOGGER.error("[HxH] Quête invalide {}", id, e);
                BROKEN.add(id + " — fichier illisible : " + e.getMessage());
            }
        });
        NPC_NAMES.clear();
        PROBLEMS.clear();
        PROBLEMS.addAll(BROKEN);
        for (Quest q : QUESTS.values()) {
            NPC_NAMES.putAll(q.npcNames());
            for (String p : q.problems()) {
                PROBLEMS.add(q.id() + " — " + p);
                HxHMod.LOGGER.warn("[HxH] Quête {} : {}", q.id(), p);
            }
        }
        HxHMod.LOGGER.info("[HxH] {} quête(s) chargée(s), {} problème(s)", QUESTS.size(), PROBLEMS.size());
    }

    /** Erreurs repérées dans les fichiers de quête (et fichiers illisibles), pour /hunter quest check. */
    private static final java.util.List<String> PROBLEMS = new java.util.ArrayList<>();
    private static final java.util.List<String> BROKEN = new java.util.ArrayList<>();
    /** Noms de PNJ déclarés dans les quêtes ("npcs"). */
    private static final Map<String, Map<String, String>> NPC_NAMES = new HashMap<>();

    /** Identifiants des PNJ déclarés dans les quêtes. */
    public static java.util.List<String> npcIds() {
        return NPC_NAMES.keySet().stream().sorted().toList();
    }

    public static java.util.List<String> problems() {
        return java.util.List.copyOf(PROBLEMS);
    }

    /** Nom d'un PNJ : fichier de langue (npc.hxh.<id>), sinon le nom déclaré dans une quête, sinon l'identifiant. */
    public static net.minecraft.network.chat.Component npcName(String npcId) {
        Map<String, String> names = NPC_NAMES.get(npcId);
        String fallback = names == null || names.isEmpty() ? npcId : Quest.pick(names, "fr_fr");
        return net.minecraft.network.chat.Component.translatableWithFallback("npc.hxh." + npcId, fallback);
    }

    /** Toutes les quêtes, triées par identifiant. */
    public static java.util.List<Quest> all() {
        return QUESTS.values().stream().sorted(java.util.Comparator.comparing(q -> q.id().toString())).toList();
    }

    @Nullable
    public static Quest get(ResourceLocation id) {
        return QUESTS.get(id);
    }
}
