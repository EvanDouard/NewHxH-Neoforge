package com.evandouard.hxh.combat;

import com.evandouard.hxh.fx.NenFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Mort des mobs sans le nuage blanc de Minecraft : au dernier tick de l'animation de chute (juste avant que le jeu ne
 * lance son nuage et retire le corps), on retire le corps nous-mêmes et on envoie une giclée d'encre dessinée.
 * Le butin et l'expérience sont déjà tombés à la mort : rien ne change pour eux.
 */
public final class DeathFx {
    /** Le jeu lance son nuage quand le compteur de mort atteint 20 : on passe avant. */
    public static final int LAST_DEATH_TICK = 19;

    private DeathFx() {}

    /** Mobs concernés : tous sauf les joueurs (jamais retirés) et le dragon (mort scénarisée). */
    public static boolean handles(Entity e) {
        return e instanceof LivingEntity && !(e instanceof Player) && !(e instanceof EnderDragon);
    }

    public static void onTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level) || !handles(e) || e.isRemoved()) return;
        LivingEntity living = (LivingEntity) e;
        if (!living.isDeadOrDying() || living.deathTime < LAST_DEATH_TICK) return;
        NenFx.dissolve(level, living);
        living.remove(Entity.RemovalReason.KILLED);
    }
}
