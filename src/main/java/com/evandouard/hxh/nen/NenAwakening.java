package com.evandouard.hxh.nen;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Les trois voies pour éveiller le Nen :
 * <ol>
 *     <li>un maître ouvre de force les pores de la cible devant lui ;</li>
 *     <li>la méditation, longue, à la manière de Netero ;</li>
 *     <li>par accident, frappé par un utilisateur de Nen.</li>
 * </ol>
 * Un éveil forcé (maître ou accident) laisse l'aura s'échapper : la cible repart à 0 et s'affaiblit.
 */
public final class NenAwakening {
    private static final Map<LivingEntity, Long> OPEN_PORES_COOLDOWN = new WeakHashMap<>();
    private static final Map<LivingEntity, Vec3> MEDITATION_START = new WeakHashMap<>();

    private NenAwakening() {}

    // ---------- 1. Le maître ouvre les pores ----------

    /**
     * Le maître ouvre les pores de la cible.
     * @return null si réussi, sinon la clé de traduction du refus
     */
    public static String openPores(LivingEntity master, @Nullable LivingEntity target) {
        HunterData m = HunterStats.get(master);
        if (!m.nenUnlocked()) return "message.hxh.nen_locked";
        long now = master.level().getGameTime();
        Long readyAt = OPEN_PORES_COOLDOWN.get(master);
        if (readyAt != null && now < readyAt) return "message.hxh.open_pores_cooldown";
        if (target == null) return "message.hxh.open_pores_no_target";
        if (HunterStats.get(target).nenUnlocked()) return "message.hxh.open_pores_already";
        int cost = HxHConfig.OPEN_PORES_AURA_COST.get();
        if (m.aura() < cost) return "message.hxh.no_aura";

        m.setAura(m.aura() - cost);
        OPEN_PORES_COOLDOWN.put(master, now + HxHConfig.OPEN_PORES_COOLDOWN_TICKS.get());
        forcedAwakening(target, "message.hxh.awakened_by_master");
        return null;
    }

    /** Version touche : vise automatiquement l'entité regardée, et prévient le joueur. */
    public static void requestOpenPores(LivingEntity master) {
        LivingEntity target = findTarget(master, HxHConfig.OPEN_PORES_RANGE.get());
        String refusal = openPores(master, target);
        if (master instanceof Player player) {
            player.displayClientMessage(refusal != null
                    ? Component.translatable(refusal)
                    : Component.translatable("message.hxh.open_pores_done", target.getDisplayName()), true);
        }
    }

    /** L'entité vivante la plus proche dans un cône de ~25° devant le regard. */
    @Nullable
    public static LivingEntity findTarget(LivingEntity source, double range) {
        Vec3 eye = source.getEyePosition();
        Vec3 look = source.getLookAngle();
        AABB box = source.getBoundingBox().inflate(range);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : source.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != source && e.isAlive())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > range + e.getBbWidth() || dist < 1e-4) continue;
            if (to.normalize().dot(look) < 0.9) continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }

    // ---------- 2. Méditation ----------

    public static void toggleMeditation(LivingEntity entity) {
        HunterData data = HunterStats.get(entity);
        if (data.meditating()) {
            stopMeditation(entity, "message.hxh.meditation_stop");
        } else {
            data.setMeditating(true);
            if (data.nenUnlocked()) data.setMeditationTicks(0);
            MEDITATION_START.put(entity, entity.position());
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable(com.evandouard.hxh.hatsu.Netero.isNetero(entity)
                        ? "message.hxh.prayer_start" : "message.hxh.meditation_start"), true);
            }
        }
    }

    private static void stopMeditation(LivingEntity entity, String messageKey) {
        HunterStats.get(entity).setMeditating(false);
        MEDITATION_START.remove(entity);
        if (entity instanceof Player player) player.displayClientMessage(Component.translatable(messageKey), true);
    }

    /** Une seconde de méditation = 20 ticks. Appelé chaque tick serveur. */
    /** Couleur de l'aura qui tourne autour du méditant, par niveau de sanctuaire (I vert pâle … V doré). */
    static final int[] SHRINE_COLORS = {0xFF9FE0A8, 0xFF7FD8FF, 0xFFB06BFF, 0xFFFF8A3C, 0xFFFFD23F};

    public static void tickMeditation(LivingEntity entity) {
        if (!entity.hasData(ModAttachments.HUNTER)) return;
        HunterData data = HunterStats.get(entity);
        if (!data.meditating()) return;

        // Bouger ou être frappé interrompt la méditation
        Vec3 start = MEDITATION_START.computeIfAbsent(entity, LivingEntity::position);
        if (entity.position().distanceToSqr(start) > 0.04 || entity.hurtTime > 0) {
            stopMeditation(entity, "message.hxh.meditation_interrupted");
            return;
        }

        if (!data.nenUnlocked()) {
            data.setMeditationTicks(data.meditationTicks() + 1);
            int needed = HxHConfig.MEDITATION_SECONDS_TO_AWAKEN.get() * 20;
            if (data.meditationTicks() >= needed) {
                NenModes.unlock(entity);
                stopMeditation(entity, "message.hxh.awakened_by_meditation");
            } else if (entity instanceof Player player && data.meditationTicks() % 200 == 0) {
                player.displayClientMessage(Component.translatable("message.hxh.meditation_progress",
                        data.meditationTicks() * 100 / needed), true);
            }
        } else {
            data.setMeditationTicks(data.meditationTicks() + 1);
            // Netero prie : sa jauge de Prière monte
            if (com.evandouard.hxh.hatsu.Netero.isNetero(entity))
                com.evandouard.hxh.hatsu.Netero.prayTick(entity, data.meditationTicks());
        }
        if (data.nenUnlocked() && entity.tickCount % 200 == 0) {
            // Déjà éveillé : la méditation entraîne la Volonté et le Nen
            // à côté d'une pierre de méditation (sanctuaire), la Volonté monte bien plus vite
            double mult = com.evandouard.hxh.meditation.MeditationShrines.willMultiplier(entity);
            HunterStats.addXp(entity, Stat.WILLPOWER, mult);
            NenProgress.addNenXp(entity, 1);
            // retour visible : XP gagnée, et le multiplicateur du sanctuaire s'il y en a un
            if (entity instanceof Player player) {
                player.displayClientMessage(mult > 1
                        ? Component.translatable("message.hxh.meditation_xp_shrine", Math.round(mult), Math.round(mult))
                        : Component.translatable("message.hxh.meditation_xp", 1), true);
            }
        }
        // Près d'une pierre de méditation : l'aura se met à tourner autour du méditant, plus fort selon le niveau
        if (data.nenUnlocked() && entity.tickCount % 20 == 0) {
            int tier = com.evandouard.hxh.meditation.MeditationShrines.stoneTier(entity);
            if (tier > 0) {
                int color = SHRINE_COLORS[tier - 1];
                com.evandouard.hxh.fx.NenFx.flare(entity, 0.6f + 0.35f * tier, color);
                if (tier >= 3) com.evandouard.hxh.fx.NenFx.shockwave(entity, 1.2f + 0.5f * tier, color);
                if (tier >= 5) com.evandouard.hxh.fx.NenFx.crackle(entity, color, 20);
            }
        }
    }

    // ---------- 3. Par accident ----------

    /**
     * Un utilisateur de Nen vient de frapper la victime : petite chance d'ouvrir ses pores.
     * @return true si l'éveil a eu lieu
     */
    public static boolean tryAccidentalAwakening(LivingEntity attacker, LivingEntity victim, RandomSource random) {
        if (!attacker.hasData(ModAttachments.HUNTER) || !HunterStats.get(attacker).nenUnlocked()) return false;
        if (HunterStats.get(victim).nenUnlocked()) return false;
        if (random.nextDouble() >= HxHConfig.ACCIDENTAL_AWAKENING_CHANCE.get()) return false;
        forcedAwakening(victim, "message.hxh.awakened_by_accident");
        return true;
    }

    // ---------- Commun ----------

    /** Éveil brutal : l'aura s'échappe d'un coup, la cible est affaiblie quelques secondes. */
    public static void forcedAwakening(LivingEntity target, String messageKey) {
        NenModes.unlock(target);
        HunterStats.get(target).setAura(0);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, false, true));
        if (target instanceof Player player) {
            player.displayClientMessage(Component.translatable(messageKey), false);
            player.displayClientMessage(Component.translatable("message.hxh.awakened_hint"), false);
        }
    }
}
