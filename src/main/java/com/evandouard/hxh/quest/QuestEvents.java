package com.evandouard.hxh.quest;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Branche les quêtes sur le jeu : connexion, ticks, mobs tués. */
public final class QuestEvents {
    private QuestEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p) QuestManager.sync(p);
        });
        bus.addListener(PlayerEvent.PlayerRespawnEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p) QuestManager.sync(p);
        });
        bus.addListener(PlayerTickEvent.Post.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p) QuestManager.tick(p);
        });
        bus.addListener(LivingDeathEvent.class, e -> {
            if (e.getSource().getEntity() instanceof ServerPlayer p) QuestManager.onKill(p, e.getEntity());
        });
    }
}
