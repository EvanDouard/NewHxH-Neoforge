package com.evandouard.hxh.summon;

import com.evandouard.hxh.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Invocations des techniques (côté serveur). Chaque invocation suit une entité ; {@link #keep} réutilise celle qui
 * suit déjà cette entité avec ce modèle (Pot-Clean rafraîchi toutes les secondes) au lieu d'en empiler.
 */
public final class NenSummons {
    /** Bodhisattva aux mille mains derrière Netero (hauteur de référence du modèle : 4 blocs). */
    public static final String KANNON = "kannon";
    /** Pot-Clean au-dessus de la cible de Knuckle (1 bloc). */
    public static final String POT_CLEAN = "pot_clean";
    /** Terpsichora, la marionnette de Pitou, derrière elle (≈ 3,4 blocs). */
    public static final String TERPSICHORA = "terpsichora";
    /** Docteur Blythe au-dessus du patient (≈ 3 blocs). */
    public static final String DOCTOR_BLYTHE = "doctor_blythe";

    private NenSummons() {}

    public static NenSummonEntity spawn(ServerLevel level, Entity owner, String model, Entity follow,
                                        NenSummonEntity.Anchor anchor, float scale, float offset, int color, int life) {
        NenSummonEntity e = ModEntities.NEN_SUMMON.get().create(level);
        if (e == null) return null;
        if (follow != null) e.setPos(follow.position());
        e.setup(owner, model, follow, anchor, scale, offset, color, life);
        level.addFreshEntity(e);
        return e;
    }

    /** L'invocation de ce modèle qui suit déjà cette entité, ou {@code null}. */
    public static NenSummonEntity find(ServerLevel level, String model, Entity follow) {
        for (NenSummonEntity e : level.getEntitiesOfClass(NenSummonEntity.class, follow.getBoundingBox().inflate(16),
                s -> s.isAlive() && s.model().equals(model) && s.followed() == follow)) return e;
        return null;
    }

    /** Prolonge l'invocation existante ou en crée une. */
    public static NenSummonEntity keep(ServerLevel level, Entity owner, String model, Entity follow,
                                       NenSummonEntity.Anchor anchor, float scale, float offset, int color, int life) {
        NenSummonEntity e = find(level, model, follow);
        if (e != null) {
            e.extend(life);
            return e;
        }
        return spawn(level, owner, model, follow, anchor, scale, offset, color, life);
    }

    /** Retire l'invocation de ce modèle qui suit cette entité (Pot-Clean annulé, banqueroute…). */
    public static void dismiss(ServerLevel level, String model, Entity follow) {
        NenSummonEntity e = find(level, model, follow);
        if (e != null) e.discard();
    }

    /** Retire toutes les invocations de ce lanceur (ses effets prennent fin : poupée relâchée, Terpsichora coupée). */
    public static void dismissOwned(ServerLevel level, Entity owner) {
        for (NenSummonEntity e : level.getEntitiesOfClass(NenSummonEntity.class, owner.getBoundingBox().inflate(32),
                s -> s.ownedBy(owner) && !s.model().equals(POT_CLEAN))) e.discard();
    }
}
