package com.evandouard.hxh.combat;

import com.evandouard.hxh.network.CombatAnimPayload;
import com.evandouard.hxh.network.HxHNetwork;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Côté serveur : choisit et diffuse les animations de combat à ceux qui voient l'entité (et à elle-même).
 * Les coups de base n'animent que les mains nues ; avec une arme, l'animation de Minecraft est gardée.
 */
public final class CombatAnimations {
    /** Écart minimal entre deux coups de poing animés (évite le double déclenchement coup + touche). */
    public static final int PUNCH_MIN_INTERVAL = 3;

    private record Last(CombatAnim anim, long tick) {}

    private static final Map<LivingEntity, Last> LAST = new WeakHashMap<>();

    private CombatAnimations() {}

    public static void play(LivingEntity entity, CombatAnim anim) {
        if (entity.level().isClientSide()) return;
        LAST.put(entity, new Last(anim, entity.level().getGameTime()));
        HxHNetwork.sendToTrackingAndSelf(entity, new CombatAnimPayload(entity.getId(), anim.ordinal()));
    }

    /**
     * Coup de base à mains nues : alterne main principale et autre main.
     * @return l'animation jouée, ou null (arme en main, ou coup trop rapproché du précédent)
     */
    public static CombatAnim punch(LivingEntity entity) {
        if (entity.level().isClientSide() || !entity.getMainHandItem().isEmpty()) return null;
        long now = entity.level().getGameTime();
        Last last = LAST.get(entity);
        if (last != null && now - last.tick() < PUNCH_MIN_INTERVAL && (last.anim().isPunch() || last.anim() == CombatAnim.CHARGED_PUNCH)) {
            return null;
        }
        // Une frappe en cours d'armement n'est pas interrompue par un coup de base
        if (last != null && last.anim() == CombatAnim.CHARGE && Combat.isCharging(entity)) return null;
        CombatAnim next = last != null && last.anim() == CombatAnim.PUNCH_MAIN && now - last.tick() <= 20
                ? CombatAnim.PUNCH_OFF : CombatAnim.PUNCH_MAIN;
        play(entity, next);
        return next;
    }

    /** Dernière animation jouée (tests). */
    public static CombatAnim last(LivingEntity entity) {
        Last l = LAST.get(entity);
        return l == null ? null : l.anim();
    }
}
