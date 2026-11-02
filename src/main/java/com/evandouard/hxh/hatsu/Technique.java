package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Une technique de Hatsu. Lancée par n'importe quelle LivingEntity (joueur via une touche, mob via son IA).
 * <p>
 * {@code power} vaut l'affinité (hexagone : catégorie de la technique vs type du lanceur)
 * multipliée par le bonus de Volonté. 1.0 = puissance nominale.
 */
public abstract class Technique {
    private final String id;
    private final NenType category;
    private final int auraCost;
    private final int cooldownTicks;

    protected Technique(String id, NenType category, int auraCost, int cooldownTicks) {
        this.id = id;
        this.category = category;
        this.auraCost = auraCost;
        this.cooldownTicks = cooldownTicks;
    }

    public String id() {
        return id;
    }

    public NenType category() {
        return category;
    }

    public int auraCost() {
        return auraCost;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public String translationKey() {
        return "technique.hxh." + id;
    }

    public String descriptionKey() {
        return "technique.hxh." + id + ".desc";
    }

    /**
     * Effet de la technique, côté serveur.
     * @return false si elle n'a pas pu s'appliquer (pas de cible) : rien n'est alors consommé
     */
    public abstract boolean perform(LivingEntity caster, float power);
}
