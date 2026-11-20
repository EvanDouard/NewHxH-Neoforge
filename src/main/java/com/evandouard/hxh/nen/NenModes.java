package com.evandouard.hxh.nen;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Logique serveur de l'aura et des modes de base (Ten, Zetsu, Ren).
 * Marche pour toute LivingEntity : un mob peut passer en Ren exactement comme un joueur.
 */
public final class NenModes {
    private NenModes() {}

    public static int computeAuraMax(HunterData data) {
        // Le talent pour le Nen multiplie tout : Prometteur ×1, Surdoué ×2…
        return (int) Math.round((HxHConfig.AURA_BASE.get() + HxHConfig.AURA_PER_WILLPOWER.get() * data.level(Stat.WILLPOWER))
                * data.talentMultiplier());
    }

    /** Débloque le Nen : aura pleine, mode neutre. */
    public static void unlock(LivingEntity entity) {
        HunterData data = HunterStats.get(entity);
        data.setNenUnlocked(true);
        if (entity instanceof net.minecraft.world.entity.Mob mob) NenMobAI.onAwakened(mob);
        data.setAuraMax(computeAuraMax(data));
        data.setAura(data.auraMax());
        data.setMode(NenMode.NONE);
    }

    /**
     * Essaie de passer dans un mode.
     * @return null si c'est accepté, sinon la clé de traduction du refus
     */
    public static String trySetMode(LivingEntity entity, NenMode mode) {
        HunterData data = HunterStats.get(entity);
        if (!data.nenUnlocked()) return "message.hxh.nen_locked";
        // Banqueroute (Hakoware de Knuckle) : Zetsu forcé
        if (mode != NenMode.ZETSU && com.evandouard.hxh.hatsu.Specials.bankrupt(entity)) return "message.hxh.bankrupt";
        NenLearning.Ability ability = NenLearning.forMode(mode);
        if (ability != null && NenLearning.refusal(entity, ability) != null) return NenLearning.refusal(entity, ability);
        if ((mode == NenMode.REN || mode == NenMode.KEN) && data.aura() < 1) return "message.hxh.no_aura";
        data.setMode(mode);
        return null;
    }

    /** Le plafond gagné en Ren redescend de tant de points d'aura par tick (vitesse fixe). */
    public static final float REN_SURGE_DECAY = 0.5f;

    /** Le Ken peut être pris maintenant (appris, assez d'aura) : sinon la touche du Ten arrête tout. */
    static boolean kenAvailable(LivingEntity entity) {
        HunterData data = HunterStats.get(entity);
        NenLearning.Ability ability = NenLearning.forMode(NenMode.KEN);
        if (ability != null && NenLearning.refusal(entity, ability) != null) return false;
        return data.aura() >= 1;
    }

    /** Le Zetsu complet se débloque en travaillant l'In. */
    public static boolean zetsuUnlocked(HunterData data) {
        return data.masteryLevel(com.evandouard.hxh.data.Mastery.IN) >= HxHConfig.ZETSU_UNLOCK_IN_LEVEL.get();
    }

    /** Zetsu débloqué : appris auprès du maître (apprentissage actif), sinon en travaillant l'In. */
    public static boolean zetsuUnlocked(LivingEntity entity) {
        if (NenLearning.applies(entity)) return HunterStats.get(entity).learned(NenLearning.Ability.ZETSU.id());
        return zetsuUnlocked(HunterStats.get(entity));
    }

    /** Touche pressée : même mode = retour au neutre, sinon on essaie le nouveau mode. */
    public static void toggle(LivingEntity entity, NenMode mode) {
        HunterData data = HunterStats.get(entity);
        NenMode target = data.mode() == mode ? NenMode.NONE : mode;
        // Touche du Ten : Ten → Ken (s'il est débloqué) → arrêt
        if (mode == NenMode.TEN) {
            if (data.mode() == NenMode.TEN) target = kenAvailable(entity) ? NenMode.KEN : NenMode.NONE;
            else if (data.mode() == NenMode.KEN) target = NenMode.NONE;
            else target = NenMode.TEN;
        }
        // Touche de dissimulation : In tant que le Zetsu n'est pas débloqué, puis seulement le Zetsu
        // (qui le remplace) ; appuyer à nouveau revient au neutre.
        if (mode == NenMode.ZETSU || mode == NenMode.IN) {
            NenMode hide = zetsuUnlocked(entity) ? NenMode.ZETSU : NenMode.IN;
            target = data.mode() == NenMode.ZETSU || data.mode() == NenMode.IN ? NenMode.NONE : hide;
        }
        String refusal = trySetMode(entity, target);
        if (refusal == null && (target == NenMode.ZETSU || target == NenMode.IN)) {
            if (target == NenMode.ZETSU) data.setHealing(false); // pas de soin sans aura
            data.setGyo(false); // dissimulé : Gyo et En coupés
            data.setEn(false);
        }
        if (entity instanceof Player player) {
            player.displayClientMessage(refusal != null
                    ? Component.translatable(refusal)
                    : Component.translatable("message.hxh.mode_changed", Component.translatable(target.translationKey())), true);
        }
    }

    /** Appelé à chaque tick serveur pour les entités ayant des données Hunter. */
    public static void tick(LivingEntity entity) {
        if (!entity.hasData(ModAttachments.HUNTER)) return;
        HunterData data = HunterStats.get(entity);
        if (!data.nenUnlocked()) return;
        // Nen scellé (prix de « Tout sacrifier ») : plus d'aura, plus de posture
        if (com.evandouard.hxh.hatsu.Vows.nenSealed(data, entity.level().getGameTime())) {
            if (data.mode() != NenMode.NONE) data.setMode(NenMode.NONE);
            if (data.gyo()) data.setGyo(false);
            if (data.en()) data.setEn(false);
            if (data.aura() > 0) data.setAura(0);
            return;
        }

        // Ren : le plafond d'aura monte (selon la maîtrise du Ren), sans donner d'aura ; après, il redescend
        // à vitesse fixe (et l'aura au-dessus du plafond retombe avec lui)
        int base = computeAuraMax(data);
        float target = data.mode() == NenMode.REN
                ? base * (0.25f + data.masteryLevel(com.evandouard.hxh.data.Mastery.REN) * 0.005f) : 0f;
        float surge = data.renSurge();
        surge = surge < target ? Math.min(target, surge + Math.max(0.5f, target / 40f)) : Math.max(target, surge - REN_SURGE_DECAY);
        data.setRenSurge(surge);
        data.setAuraMax(base + Math.round(surge));
        NenDefense.tickKo(entity, data);
        float regen = (float) (HxHConfig.AURA_REGEN_PER_SECOND.get() / 20.0);
        if (data.meditating()) regen *= 2; // la méditation recharge l'aura plus vite
        // Niveaux 0 à 4 de la régénération : aucune régénération (seul le sommeil rend l'aura) ; les mobs et les
        // entités sans règle d'apprentissage gardent leur régénération normale
        if (NenLearning.applies(entity)) regen *= (float) NenCost.regenFactor(data.masteryLevel(com.evandouard.hxh.data.Mastery.REGEN));
        if (entity instanceof net.minecraft.server.level.ServerPlayer sp) sleepAndEmpty(sp, data);
        if (entity.hasEffect(com.evandouard.hxh.effect.ModEffects.NEN_CURSE)) regen *= 0.5f; // malédiction de Nen

        switch (data.mode()) {
            case NONE -> data.setAura(data.aura() + regen);
            case TEN, IN -> {
                // Débutant : tenir la posture coûte cher (plus que la régénération), de moins en moins jusqu'au niveau 5
                com.evandouard.hxh.data.Mastery m = data.mode() == NenMode.TEN ? com.evandouard.hxh.data.Mastery.TEN : com.evandouard.hxh.data.Mastery.IN;
                // (les mobs ne progressent pas : leur maîtrise fixée à la main ne les prive pas de leur posture)
                float drain = NenLearning.applies(entity) ? (float) (NenCost.novicePostureDrain(data.masteryLevel(m)) / 20.0) : 0f;
                data.setAura(data.aura() + regen - drain);
                if (drain > 0 && data.aura() <= 0) {
                    data.setMode(NenMode.NONE);
                    if (entity instanceof Player player) player.displayClientMessage(Component.translatable("message.hxh.posture_empty"), true);
                }
            }
            case ZETSU -> data.setAura(data.aura() + regen * (float) (HxHConfig.ZETSU_REGEN_MULTIPLIER.get()
                    + data.masteryLevel(com.evandouard.hxh.data.Mastery.ZETSU) * 0.02));
            case REN -> {
                data.setAura(data.aura() - (float) (HxHConfig.REN_DRAIN_PER_SECOND.get()
                        * NenCost.factor(data, com.evandouard.hxh.data.Mastery.REN) / 20.0));
                if (data.aura() <= 0) exhaust(entity, data);
            }
            case KEN -> {
                double drain = Math.max(1.5, HxHConfig.KEN_DRAIN_PER_SECOND.get()
                        - data.masteryLevel(com.evandouard.hxh.data.Mastery.KEN) * 0.02);
                data.setAura(data.aura() - (float) (drain * NenCost.factor(data, com.evandouard.hxh.data.Mastery.KEN) / 20.0));
                if (data.aura() <= 0) exhaust(entity, data);
            }
        }

        // Entraînement : chaque seconde passée dans une posture fait progresser sa maîtrise et le Nen
        if (entity.tickCount % 20 == 0) {
            switch (data.mode()) {
                case TEN -> NenProgress.train(entity, com.evandouard.hxh.data.Mastery.TEN, 1);
                case ZETSU -> NenProgress.train(entity, com.evandouard.hxh.data.Mastery.ZETSU, 1);
                case IN -> NenProgress.train(entity, com.evandouard.hxh.data.Mastery.IN, 1);
                case REN -> {
                    NenProgress.train(entity, com.evandouard.hxh.data.Mastery.REN, 2);
                    HunterStats.addXp(entity, Stat.WILLPOWER, 1);
                }
                case KEN -> {
                    NenProgress.train(entity, com.evandouard.hxh.data.Mastery.KEN, 2);
                    HunterStats.addXp(entity, Stat.WILLPOWER, 1);
                }
                default -> { }
            }
        }
    }

    /** Joueurs ayant déjà été rechargés par le sommeil en cours ; joueurs prévenus que leur aura est à plat. */
    private static final java.util.Set<java.util.UUID> SLEPT = new java.util.HashSet<>(), EMPTY_TOLD = new java.util.HashSet<>();
    /** XP de régénération d'aura gagnée en dormant avec l'aura à plat (au prorata de l'aura manquante). */
    public static final int SLEEP_REGEN_XP = 80;

    /** Dormir assez longtemps rend toute l'aura et fait progresser la régénération ; aura à zéro : on le dit. */
    private static void sleepAndEmpty(net.minecraft.server.level.ServerPlayer player, HunterData data) {
        java.util.UUID id = player.getUUID();
        if (!player.isSleeping()) SLEPT.remove(id);
        else if (player.isSleepingLongEnough() && SLEPT.add(id)) {
            float missing = data.auraMax() > 0 ? 1f - data.aura() / data.auraMax() : 0f;
            data.setAura(data.auraMax());
            if (NenLearning.applies(player) && missing > 0.02f) {
                NenProgress.trainOnly(player, com.evandouard.hxh.data.Mastery.REGEN, Math.max(1, Math.round(SLEEP_REGEN_XP * missing)));
            }
            player.sendSystemMessage(Component.translatable("message.hxh.sleep_refill").withStyle(net.minecraft.ChatFormatting.AQUA));
        }
        if (data.aura() <= 0.0001f) {
            if (EMPTY_TOLD.add(id)) {
                player.sendSystemMessage(Component.translatable(data.masteryLevel(com.evandouard.hxh.data.Mastery.REGEN) < NenCost.REGEN_FROM
                        ? "message.hxh.aura_empty_sleep" : "message.hxh.aura_empty").withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
        } else if (data.aura() > data.auraMax() * 0.05f) EMPTY_TOLD.remove(id);
    }

    private static void exhaust(LivingEntity entity, HunterData data) {
        data.setMode(NenMode.NONE);
        int ticks = HxHConfig.EXHAUSTION_TICKS.get();
        if (ticks > 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, false, false, true));
        }
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.hxh.exhausted"), true);
        }
    }
}
