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
        // Voix noire de Shalnark : les mobs contrôlés attaquent ses ennemis, jamais lui
        bus.addListener(net.neoforged.neoforge.event.tick.EntityTickEvent.Post.class, e -> {
            if (e.getEntity() instanceof net.minecraft.world.entity.Mob m && !m.level().isClientSide()) Specials.tickControlled(m);
        });
        bus.addListener(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent.class, e -> {
            if (e.getEntity() instanceof net.minecraft.world.entity.Mob m && Specials.forbiddenTarget(m, e.getNewAboutToBeSetTarget()))
                e.setCanceled(true);
        });
        // Technicien (Shalnark) : un objet de plus, parfois
        bus.addListener(net.neoforged.neoforge.event.entity.living.LivingDropsEvent.class, e -> {
            if (e.getSource().getEntity() instanceof LivingEntity k && "shalnark".equals(com.evandouard.hxh.stats.HunterStats.get(k).kit())
                    && !e.getDrops().isEmpty() && k.getRandom().nextInt(4) == 0) {
                var copy = e.getDrops().iterator().next();
                e.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(copy.level(), copy.getX(), copy.getY(), copy.getZ(),
                        copy.getItem().copyWithCount(1)));
            }
        });
        bus.addListener(LivingEntityUseItemEvent.Finish.class, e -> {
            if (!e.getEntity().level().isClientSide()) Characters.onEat(e.getEntity(), e.getItem());
        });
    }
}
