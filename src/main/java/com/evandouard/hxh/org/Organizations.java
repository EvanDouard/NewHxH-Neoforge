package com.evandouard.hxh.org;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/** Charge les organisations ({@code organizations}) ; rechargées par /reload. Adhésion, départ, alliés et ennemis. */
public final class Organizations extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, Organization> ORGS = Map.of();

    private Organizations() {
        super(new Gson(), "organizations");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new Organizations());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, Organization> loaded = new TreeMap<>();
        files.forEach((id, json) -> {
            try {
                loaded.put(id.getPath(), Organization.parse(id, json.getAsJsonObject()));
            } catch (Exception e) {
                HxHMod.LOGGER.error("Organisation {} invalide : {}", id, e.getMessage());
            }
        });
        ORGS = Map.copyOf(loaded);
        HxHMod.LOGGER.info("{} organisation(s) chargée(s)", loaded.size());
    }

    @Nullable
    public static Organization get(String id) {
        return id == null || id.isEmpty() ? null : ORGS.get(id);
    }

    public static Map<String, Organization> all() {
        return ORGS;
    }

    /** Organisation du joueur, ou null. */
    @Nullable
    public static Organization of(Player player) {
        return player instanceof ServerPlayer sp ? get(QuestManager.data(sp).organization()) : null;
    }

    /** Deux joueurs dans des organisations ennemies : leurs techniques se touchent. */
    public static boolean enemies(Player a, Player b) {
        if (!(a instanceof ServerPlayer sa) || !(b instanceof ServerPlayer sb)) return false;
        Organization oa = get(QuestManager.data(sa).organization()), ob = get(QuestManager.data(sb).organization());
        if (oa == null || ob == null) return false;
        return oa.isEnemy(ob.id()) || ob.isEnemy(oa.id());
    }

    /** Récompense de quête {@code reward_join} : on rejoint l'organisation (et on quitte l'ancienne). */
    public static void join(ServerPlayer player, String id) {
        Organization org = get(id);
        if (org == null) {
            HxHMod.LOGGER.warn("Organisation inconnue dans une récompense : {}", id);
            return;
        }
        QuestData d = QuestManager.data(player);
        Organization old = get(d.organization());
        if (old != null && !old.id().equals(id)) {
            player.sendSystemMessage(Component.translatable("org.hxh.left", label(old, player)).withStyle(ChatFormatting.GRAY));
        }
        d.setOrganization(id);
        com.evandouard.hxh.network.Hud.toast(player, Component.translatable("org.hxh.toast.joined"), label(org, player), "◆",
                com.evandouard.hxh.network.Hud.GOLD);
        if (!org.enemies().isEmpty()) {
            player.sendSystemMessage(Component.translatable("org.hxh.enemies", String.join(", ",
                    org.enemies().stream().map(e -> get(e) != null ? get(e).name(player.clientInformation().language()) : e).toList()))
                    .withStyle(ChatFormatting.RED));
        }
    }

    public static boolean leave(ServerPlayer player) {
        QuestData d = QuestManager.data(player);
        Organization old = get(d.organization());
        if (old == null) {
            player.sendSystemMessage(Component.translatable("org.hxh.none").withStyle(ChatFormatting.GRAY));
            return false;
        }
        d.setOrganization("");
        player.sendSystemMessage(Component.translatable("org.hxh.left", label(old, player)).withStyle(ChatFormatting.YELLOW));
        return true;
    }

    public static Component label(Organization org, ServerPlayer player) {
        return Component.literal(org.name(player.clientInformation().language())).withStyle(s -> s.withColor(org.color()));
    }
}
