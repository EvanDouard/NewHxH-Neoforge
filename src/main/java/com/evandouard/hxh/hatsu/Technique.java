package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;
import net.minecraft.network.chat.Component;
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
    private int minNenLevel = 0;

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

    /** Niveau de Nen minimum pour pouvoir lancer cette technique. */
    public int minNenLevel() {
        return minNenLevel;
    }

    public Technique requiresNenLevel(int level) {
        this.minNenLevel = level;
        return this;
    }

    public String translationKey() {
        return "technique.hxh." + id;
    }

    public String descriptionKey() {
        return "technique.hxh." + id + ".desc";
    }

    /** false pour une technique encore à l'état d'idée (décrite, pas jouable). */
    public boolean implemented() {
        return true;
    }

    /** Distance à laquelle un mob tente cette technique (infini = sur soi, utilisable partout). */
    public double aiRange() {
        return Double.MAX_VALUE;
    }

    /** Technique qu'on charge en maintenant la touche ({@link NenCharge}) : la recharge part au relâchement. */
    public boolean isCharged() {
        return false;
    }

    /** Technique de soin : un mob ne la lance que s'il est blessé. */
    public boolean isHeal() {
        return false;
    }

    /** Conditions d'utilisation affichées dans l'écran des techniques (portée, cible…). */
    public Component conditions() {
        return Component.translatable("technique.hxh.cond.self");
    }

    /**
     * Effet de la technique, côté serveur.
     * @return false si elle a raté (pas de cible) : l'aura et la recharge sont quand même consommées
     */
    public abstract boolean perform(LivingEntity caster, float power);
}
