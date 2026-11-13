package com.evandouard.hxh.combat;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.hatsu.NenDamageSource;
import com.evandouard.hxh.hatsu.Technique;
import com.evandouard.hxh.hatsu.Techniques;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Coups qui deviennent des attaques de zone quand on est assez fort :
 * <ul>
 *   <li>coups de base (poing, coup de pied, frappe chargée) au-delà de {@link #BASE_THRESHOLD} en
 *       Force + Vitesse + Endurance ;</li>
 *   <li>techniques de Nen au-delà de {@link #NEN_THRESHOLD} en Volonté + niveau de Nen, sauf les techniques
 *       à cible unique ({@link #SINGLE_TARGET}).</li>
 * </ul>
 * Les ennemis autour de la cible prennent {@link #SPLASH} des dégâts ; le rayon grandit avec l'excédent.
 * Vaut pour toute {@code LivingEntity} (un mob assez fort frappe aussi en zone).
 */
public final class AreaScaling {
    private AreaScaling() {}

    /** Seuils (sommes de niveaux, 300 et 200 au maximum hors serments). Valeurs pour l'équilibrage final. */
    public static final int BASE_THRESHOLD = 180, NEN_THRESHOLD = 120;
    /** Part des dégâts reçue par les voisins, rayon au seuil et rayon maximal. */
    public static final float SPLASH = 0.5f;
    public static final double MIN_RADIUS = 1.5, MAX_RADIUS = 4.0;

    /** Techniques à cible unique, jamais en zone (liens, aiguilles, contrats de Nen…). */
    public static final Set<String> SINGLE_TARGET = Set.of(
            "illumi_needle", "kurapika_chain_jail", "machi_nen_threads", "pakunoda_memory_bomb", "hisoka_bungee_gum",
            "knuckle_hakoware", "debt", "pariston_love_debt", "chrollo_skill_hunter", "shalnark_black_voice",
            "ging_copy", "arena_riehlvelt_whip", "arena_sadaso_ghost_hand", "netero_zero_hand", "genthru_countdown",
            "pitou_doctor_blythe");

    /** Technique en cours de lancement par chaque lanceur (pour savoir si ses dégâts peuvent déborder). */
    private static final Map<LivingEntity, Technique> CASTING = new WeakHashMap<>();
    /** Une victime ne prend qu'un éclat par tick (évite qu'une onde déjà en zone se démultiplie). */
    private static final Map<LivingEntity, Long> SPLASHED = new WeakHashMap<>();
    private static boolean splashing;

    public static void register(IEventBus bus) {
        bus.addListener(LivingDamageEvent.Post.class, AreaScaling::onDamage);
    }

    /** Appelé juste avant {@code perform} : les dégâts de cette technique pourront déborder. */
    public static void casting(LivingEntity caster, Technique technique) {
        CASTING.put(caster, technique);
    }

    public static boolean canSpread(Technique t) {
        return !t.isBase() && !t.isFamily() && !t.isHeal() && !SINGLE_TARGET.contains(t.id());
    }

    static double radiusFor(int sum, int threshold, int max) {
        if (sum < threshold) return 0;
        double k = (sum - threshold) / (double) Math.max(1, max - threshold);
        return Math.min(MAX_RADIUS, MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * k);
    }

    /** Rayon de zone des coups de base (0 = pas de zone). */
    public static double baseRadius(LivingEntity e) {
        HunterData d = HunterStats.get(e);
        int sum = d.level(Stat.STRENGTH) + d.level(Stat.SPEED) + d.level(Stat.ENDURANCE);
        return radiusFor(sum, BASE_THRESHOLD, 3 * Stat.MAX_LEVEL);
    }

    /** Rayon de zone des techniques de Nen (0 = pas de zone). */
    public static double nenRadius(LivingEntity e) {
        HunterData d = HunterStats.get(e);
        int sum = d.level(Stat.WILLPOWER) + d.nenLevel();
        return radiusFor(sum, NEN_THRESHOLD, Stat.MAX_LEVEL + 100);
    }

    /** Coup de base qui vient de toucher {@code target} : éclats autour si l'attaquant est assez fort. */
    public static int baseSplash(LivingEntity attacker, LivingEntity target, float damage, DamageSource source) {
        double r = baseRadius(attacker);
        return r <= 0 ? 0 : splash(attacker, target, damage, r, source, NenFx.WHITE);
    }

    private static void onDamage(LivingDamageEvent.Post event) {
        if (splashing || !(event.getSource() instanceof NenDamageSource src)) return;
        if (!(src.getEntity() instanceof LivingEntity caster) || caster.level().isClientSide()) return;
        Technique t = CASTING.get(caster);
        if (t == null || !canSpread(t)) return;
        double r = nenRadius(caster);
        if (r > 0) splash(caster, event.getEntity(), event.getNewDamage(), r, Techniques.damageFrom(caster), HunterStats.get(caster).auraColor());
    }

    /** Blesse les ennemis autour de {@code target} (pas l'attaquant, ni ses animaux). @return le nombre touché */
    public static int splash(LivingEntity attacker, LivingEntity target, float damage, double radius, DamageSource source, int color) {
        if (damage <= 0 || splashing) return 0;
        long now = attacker.level().getGameTime();
        int hit = 0;
        splashing = true;
        try {
            for (LivingEntity other : attacker.level().getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(radius), o -> o != attacker && o != target && o.isAlive()
                            && o.distanceTo(target) <= radius && !isAlly(attacker, o))) {
                Long last = SPLASHED.get(other);
                if (last != null && last == now) continue;
                SPLASHED.put(other, now);
                other.invulnerableTime = 0;
                if (other.hurt(source, damage * SPLASH)) hit++;
            }
        } finally {
            splashing = false;
        }
        if (hit > 0) NenFx.shockwave(target, (float) radius, color);
        return hit;
    }

    private static boolean isAlly(LivingEntity attacker, LivingEntity other) {
        if (other instanceof TamableAnimal pet && pet.getOwner() == attacker) return true;
        return attacker.isAlliedTo(other);
    }

    @Nullable
    static Technique castingOf(LivingEntity caster) {
        return CASTING.get(caster);
    }
}
