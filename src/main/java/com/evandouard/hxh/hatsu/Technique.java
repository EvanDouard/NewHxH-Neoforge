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
    /** Ressource propre au personnage consommée en plus de l'aura (Prière de Netero…), et sa quantité. */
    private String resource = null;
    private float resourceCost = 0f;

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

    /** Niveau de Nen requis à partir duquel une technique compte comme ultime. */
    public static final int ULTIMATE_NEN_LEVEL = 30;

    /** Technique ultime (Jajanken épique, Plongée du dragon, Photon de Meruem…) : jamais pour une copie portée par un mob. */
    public boolean isUltimate() {
        return minNenLevel >= ULTIMATE_NEN_LEVEL;
    }

    public Technique requiresNenLevel(int level) {
        this.minNenLevel = level;
        return this;
    }

    /** La technique coûte aussi {@code amount} de la ressource du personnage {@code key} (voir {@link KitBars}). */
    public Technique costs(String key, float amount) {
        this.resource = key;
        this.resourceCost = amount;
        return this;
    }

    public String resource() {
        return resource;
    }

    public float resourceCost() {
        return resourceCost;
    }

    /** Refus propre à la technique (au-delà de l'aura, de la recharge…), ou null. */
    public String extraRefusal(com.evandouard.hxh.data.HunterData data) {
        if (resource != null && data.kitValue(resource) + 1e-3f < resourceCost) return "message.hxh.no_" + resource;
        return null;
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

    /** Coup de base (sans Nen, disponible dès le début, gratuit) : sa recharge baisse avec l'Endurance. */
    public boolean isBase() {
        return false;
    }

    /** Coup secret de famille : sans Nen, comme un coup de base, mais propre au sang (ni copié, ni volé). */
    public boolean isFamily() {
        return false;
    }

    /** Technique qui part sans Nen : coups de base et coups secrets de famille. */
    public boolean needsNoNen() {
        return isBase() || isFamily();
    }

    /** Recharge réelle pour cette entité (les coups de base raccourcissent avec l'Endurance). */
    public int cooldownFor(com.evandouard.hxh.data.HunterData data) {
        return cooldownTicks;
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
