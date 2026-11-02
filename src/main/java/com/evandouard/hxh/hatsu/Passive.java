package com.evandouard.hxh.hatsu;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Propriété passive d'un personnage : toujours active tant qu'on a choisi ce Hatsu
 * (et qu'on n'est pas en Zetsu pour celles qui dépendent de l'aura). Marche aussi pour les mobs.
 */
public class Passive {
    private final String id;
    private final boolean implemented;

    protected Passive(String id, boolean implemented) {
        this.id = id;
        this.implemented = implemented;
    }

    /** Passif décrit seulement (personnages à l'état d'idée). */
    public static Passive idea(String id) {
        return new Passive(id, false);
    }

    public String id() {
        return id;
    }

    public boolean implemented() {
        return implemented;
    }

    public String translationKey() {
        return "passive.hxh." + id;
    }

    public String descriptionKey() {
        return "passive.hxh." + id + ".desc";
    }

    /** Chaque tick serveur. */
    public void tick(LivingEntity owner) {}

    /** Dégâts reçus par le porteur. */
    public float modifyIncoming(LivingEntity owner, DamageSource source, float amount) {
        return amount;
    }

    /** Dégâts infligés au corps à corps par le porteur. */
    public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
        return amount;
    }
}
