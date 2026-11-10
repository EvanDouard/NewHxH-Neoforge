package com.evandouard.hxh.hatsu;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/** Événements du jeu dont dépendent certains personnages (Règlement de Cheadle, Roi de Meruem, repas de Menchi…). */
public final class CharacterEvents {
    private CharacterEvents() {}

    public static void register(IEventBus bus) {
        // Règlement de Cheadle : pas de téléportation dans la zone (perles, chorus…)
        bus.addListener(EntityTeleportEvent.class, e -> {
            if (e.getEntity() instanceof LivingEntity l
                    && l.getPersistentData().getLong(Characters.RULES_KEY) > l.level().getGameTime()
                    && !(e instanceof EntityTeleportEvent.TeleportCommand) && !(e instanceof EntityTeleportEvent.SpreadPlayersCommand)) {
                e.setCanceled(true);
            }
        });
        bus.addListener(LivingDeathEvent.class, e -> {
            if (!e.getEntity().level().isClientSide() && e.getSource().getEntity() instanceof LivingEntity killer)
                Characters.onKill(killer);
        });
        bus.addListener(LivingEntityUseItemEvent.Finish.class, e -> {
            if (!e.getEntity().level().isClientSide()) Characters.onEat(e.getEntity(), e.getItem());
        });
    }
}
