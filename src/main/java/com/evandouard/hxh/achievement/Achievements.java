package com.evandouard.hxh.achievement;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.airship.Airship;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.stats.HunterStats;
import com.evandouard.hxh.world.HxHWorldMap;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Succès : des advancements vanilla (data/hxh/advancement, générés par tools/gen_achievements.py) au critère
 * « impossible », accordés ici selon l'état du joueur : monstres vaincus, régions découvertes, objets
 * fabriqués ou achetés, jalons de Nen, étages de la Tour, licence de Hunter.
 * Le contrôle est périodique (toutes les 2 s) et ne coûte presque rien ; les achats/fabrications sont immédiats.
 */
public final class Achievements {
    private Achievements() {}

    public static final String REGIONS = "mksKobp";
    private static final int PERIOD = 40;

    public static void register(IEventBus bus) {
        bus.addListener(PlayerTickEvent.Post.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % PERIOD == 0) check(p);
        });
        bus.addListener(PlayerEvent.ItemCraftedEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p
                    && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getCrafting().getItem()).getNamespace().equals(HxHMod.MODID))
                grant(p, "first_craft");
        });
    }

    /** Accorde le succès {@code hxh:<id>} s'il existe et n'est pas encore obtenu. @return vrai s'il vient d'être accordé */
    public static boolean grant(ServerPlayer player, String id) {
        AdvancementHolder holder = player.server.getAdvancements().get(HxHMod.id(id));
        if (holder == null || player.getAdvancements().getOrStartProgress(holder).isDone()) return false;
        return player.getAdvancements().award(holder, "done");
    }

    public static boolean has(ServerPlayer player, String id) {
        AdvancementHolder holder = player.server.getAdvancements().get(HxHMod.id(id));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** Passe en revue tous les succès fondés sur l'état du joueur. */
    public static void check(ServerPlayer p) {
        QuestData q = QuestManager.data(p);
        HunterData d = HunterStats.get(p);
        if (q.isHunter()) grant(p, "hunter_license");

        int kills = p.getStats().getValue(Stats.CUSTOM.get(Stats.MOB_KILLS));
        if (kills >= 1) grant(p, "first_kill");
        if (kills >= 50) grant(p, "kills_50");
        if (kills >= 500) grant(p, "kills_500");

        if (d.nenUnlocked()) {
            grant(p, "nen_awakened");
            for (int lvl : new int[]{10, 25, 50}) if (d.nenLevel() >= lvl) grant(p, "nen_level_" + lvl);
        }

        int floor = q.arenaFloor();
        for (int f : new int[]{50, 100, 200}) if (floor >= f) grant(p, "tower_floor_" + f);
        if (floor >= com.evandouard.hxh.arena.ArenaRules.TOP_FLOOR) grant(p, "tower_top");

        if (p.level().dimension().equals(Airship.WORLD)) {
            char c = HxHWorldMap.get().cellAt(p.getBlockX(), p.getBlockZ());
            if (REGIONS.indexOf(c) >= 0) grant(p, "region_" + c);
        }
        boolean all = true;
        for (char c : REGIONS.toCharArray()) all &= has(p, "region_" + c);
        if (all) grant(p, "explorer");
    }
}
