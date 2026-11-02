package com.evandouard.hxh.stats;

import com.evandouard.hxh.HxHGameRules;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.network.HxHNetwork;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.nen.NenAwakening;
import com.evandouard.hxh.nen.NenMobAI;
import com.evandouard.hxh.nen.NenHealing;
import com.evandouard.hxh.nen.NenModes;
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
import net.neoforged.neoforge.event.tick.EntityTickEvent;
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
        gameBus.addListener(HunterEvents::onStartTracking);
        gameBus.addListener(HunterEvents::onEntityTick);
    }

    /** Réapplique les effets des stats à chaque arrivée dans un monde (connexion, réapparition, dimension). */
    private static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof LivingEntity living
                && (living instanceof Player || living.hasData(ModAttachments.HUNTER))) {
            HunterStats.applyAttributes(living);
            // Un mob à Nen rechargé du disque retrouve son comportement de combat
            if (living instanceof net.minecraft.world.entity.Mob mob && HunterStats.get(mob).nenUnlocked()) {
                mob.setPersistenceRequired();
                NenMobAI.ensureGoals(mob);
            }
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
        NenModes.tick(player);
        NenHealing.tick(player);
        NenAwakening.tickMeditation(player);
        Hatsu.tickPassives(player);
        HxHNetwork.syncDirty(player);
    }

    /** Les mobs avec des données Hunter gèrent aussi leur aura. */
    private static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)
                && !living.level().isClientSide() && living.hasData(ModAttachments.HUNTER)) {
            if (living instanceof net.minecraft.world.entity.Mob mob) NenMobAI.tick(mob);
            NenModes.tick(living);
            NenHealing.tick(living);
            NenAwakening.tickMeditation(living);
            Hatsu.tickPassives(living);
            HxHNetwork.syncDirty(living);
        }
    }

    /** Un joueur commence à voir une entité : on lui envoie ses données (pour afficher son aura). */
    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof LivingEntity target) {
            HxHNetwork.syncTo(viewer, target);
        }
    }

    /** Endurance et mode de la victime, Ren de l'attaquant au corps à corps. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        float factor = 1f;
        if (victim.hasData(ModAttachments.HUNTER)) {
            factor *= HunterStats.damageTakenFactor(victim) * NenModes.incomingFactor(victim);
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity livingAttacker && attacker == event.getSource().getDirectEntity()) {
            factor *= NenModes.outgoingFactor(livingAttacker);
        }
        float amount = event.getAmount() * factor;
        // Passifs du personnage : de la victime (immunités…) puis de l'attaquant au corps à corps
        amount = Hatsu.modifyIncoming(victim, event.getSource(), amount);
        if (attacker instanceof LivingEntity livingAttacker && attacker == event.getSource().getDirectEntity()) {
            amount = Hatsu.modifyOutgoingMelee(livingAttacker, victim, amount);
        }
        if (amount != event.getAmount()) event.setAmount(amount);
    }

    /** Force (attaquant au corps à corps) et Endurance (victime) gagnent de l'XP. Joueurs seulement. */
    private static void onDamageDealt(LivingDamageEvent.Post event) {
        float damage = event.getNewDamage();
        if (damage <= 0 || event.getEntity().level().isClientSide()) return;
        Entity attacker = event.getSource().getEntity();
        boolean melee = attacker != null && attacker == event.getSource().getDirectEntity();
        if (melee && attacker instanceof LivingEntity livingAttacker && trains(livingAttacker)) {
            HunterStats.addXp(livingAttacker, Stat.STRENGTH, damage);
        }
        // Un mob passif qui a le Nen devient neutre : il riposte contre son agresseur
        if (event.getEntity() instanceof net.minecraft.world.entity.Mob victimMob
                && !(victimMob instanceof net.minecraft.world.entity.monster.Enemy)
                && attacker instanceof LivingEntity livingAggressor && livingAggressor != victimMob
                && trains(victimMob)) {
            victimMob.setTarget(livingAggressor);
        }
        if (!(event.getEntity() instanceof Player) && trains(event.getEntity())) {
            HunterStats.addXp(event.getEntity(), Stat.ENDURANCE, damage); // les mobs à Nen progressent aussi
        }
        if (event.getEntity() instanceof Player victim) {
            HunterStats.addXp(victim, Stat.ENDURANCE, damage);
            // Frappé par un utilisateur de Nen : ses pores peuvent s'ouvrir par accident
            if (attacker instanceof LivingEntity livingAttacker) {
                NenAwakening.tryAccidentalAwakening(livingAttacker, victim, victim.getRandom());
            }
        }
    }

    /** Vitalité : gagne de l'XP en se soignant. */
    private static void onHeal(LivingHealEvent event) {
        LivingEntity e = event.getEntity();
        if (!e.level().isClientSide() && event.getAmount() > 0 && trains(e)) {
            HunterStats.addXp(e, Stat.VITALITY, event.getAmount());
        }
    }

    /** Qui gagne de l'XP de stats par l'usage : les joueurs et les mobs qui ont le Nen. */
    private static boolean trains(LivingEntity e) {
        return e instanceof Player || (e.hasData(ModAttachments.HUNTER) && HunterStats.get(e).nenUnlocked());
    }
}
