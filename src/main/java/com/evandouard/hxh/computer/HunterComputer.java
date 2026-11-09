package com.evandouard.hxh.computer;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.network.HxHNetwork;
import com.evandouard.hxh.network.OpenComputerPayload;
import com.evandouard.hxh.quest.Quest;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.stats.HunterStats;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Ordinateur Hunter : les fiches ({@code hunter_computer}) se débloquent au fil du jeu. Le serveur décide ce qui est
 * lisible et n'envoie jamais le texte d'une fiche verrouillée (seulement son indice).
 */
public final class HunterComputer extends SimpleJsonResourceReloadListener {
    private static volatile List<ComputerEntry> ENTRIES = List.of();

    private HunterComputer() {
        super(new Gson(), "hunter_computer");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new HunterComputer());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ComputerEntry> loaded = new TreeMap<>();
        files.forEach((id, json) -> {
            try {
                loaded.put(id, ComputerEntry.parse(id, json.getAsJsonObject()));
            } catch (Exception e) {
                HxHMod.LOGGER.error("Fiche d'ordinateur {} invalide : {}", id, e.getMessage());
            }
        });
        ENTRIES = List.copyOf(loaded.values());
        HxHMod.LOGGER.info("{} fiche(s) d'ordinateur Hunter chargée(s)", loaded.size());
    }

    public static List<ComputerEntry> entries() {
        return ENTRIES;
    }

    /** Fiches telles que le joueur les voit, triées par catégorie puis ordre. */
    public static List<OpenComputerPayload.Entry> view(ServerPlayer player) {
        String lang = player.clientInformation().language().toLowerCase(Locale.ROOT);
        QuestData q = QuestManager.data(player);
        HunterData h = HunterStats.get(player);
        boolean learningOff = !player.serverLevel().getGameRules().getBoolean(com.evandouard.hxh.HxHGameRules.NEN_LEARNING);
        List<OpenComputerPayload.Entry> out = new ArrayList<>();
        List<ComputerEntry> sorted = new ArrayList<>(ENTRIES);
        sorted.sort(Comparator.comparingInt(ComputerEntry::categoryOrder)
                .thenComparing((ComputerEntry e) -> pick(e.category(), lang)).thenComparingInt(ComputerEntry::order));
        for (ComputerEntry e : sorted) {
            boolean open = e.unlocked(q, h, learningOff);
            out.add(new OpenComputerPayload.Entry(e.id().getPath(), pick(e.category(), lang),
                    open ? pick(e.title(), lang) : "???", open ? pick(e.text(), lang) : "", open,
                    open ? "" : pick(e.hint(), lang)));
        }
        return out;
    }

    public static void open(ServerPlayer player) {
        HxHNetwork.sendTo(player, new OpenComputerPayload(QuestManager.data(player).isHunter(), view(player)));
    }

    private static String pick(Map<String, String> texts, String lang) {
        return texts.isEmpty() ? "" : Quest.pick(texts, lang);
    }
}
