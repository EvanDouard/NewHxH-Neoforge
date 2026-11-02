package com.evandouard.hxh.stats;

import com.evandouard.hxh.HxHGameRules;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.network.HxHNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Événements de jeu liés aux stats : gain d'XP par l'usage, effets, synchro.
 */
public final class HunterEvents {
    private HunterEvents() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(HunterEvents::onJoinLevel);
        gameBus.addListener(HunterEvents::onLogin);
        gameBus.addListener(HunterEvents::onRespawn);
        gameBus.addListener(HunterEvents::onChangeDimension);
        gameBus.addListener(HunterEvents::onPlayerTick);
        gameBus.addListener(HunterEvents::onIncomingDamage);
        gameBus.addListener(HunterEvents::onDamageDealt);
        gameBus.addListener(HunterEvents::onHeal);
    }

    /** Réapplique les effets des stats à chaque arrivée dans un monde (connexion, réapparition, dimension). */
    private static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof LivingEntity living
                && (living instanceof Player || living.hasData(ModAttachments.HUNTER))) {
            HunterStats.applyAttributes(living);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        HunterData data = HunterStats.get(player);
        if (data.nenType() == null && player.serverLevel().getGameRules().getBoolean(HxHGameRules.RANDOM_NEN_TYPE)) {
            data.setNenType(NenType.random(player.getRandom()));
        }
        HxHNetwork.syncFull(player);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) HxHNetwork.syncFull(player);
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) HxHNetwork.syncFull(player);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Vitesse : 1 XP par seconde de sprint au sol
        if (player.tickCount % 20 == 0 && player.isSprinting() && player.onGround()) {
            HunterStats.addXp(player, Stat.SPEED, 1);
        }
        HxHNetwork.syncDirty(player);
    }

    /** Endurance : réduit les dégâts reçus. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !victim.hasData(ModAttachments.HUNTER)) return;
        event.setAmount(event.getAmount() * HunterStats.damageTakenFactor(victim));
    }

    /** Force (attaquant au corps à corps) et Endurance (victime) gagnent de l'XP. Joueurs seulement. */
    private static void onDamageDealt(LivingDamageEvent.Post event) {
        float damage = event.getNewDamage();
        if (damage <= 0 || event.getEntity().level().isClientSide()) return;
        Entity attacker = event.getSource().getEntity();
        boolean melee = attacker != null && attacker == event.getSource().getDirectEntity();
        if (melee && attacker instanceof Player player) {
            HunterStats.addXp(player, Stat.STRENGTH, damage);
        }
        if (event.getEntity() instanceof Player victim) {
            HunterStats.addXp(victim, Stat.ENDURANCE, damage);
        }
    }

    /** Vitalité : gagne de l'XP en se soignant. */
    private static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0) {
            HunterStats.addXp(player, Stat.VITALITY, event.getAmount());
        }
    }
}
