package com.evandouard.hxh.nen;

import com.evandouard.hxh.HxHGameRules;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Apprentissage du Nen auprès d'un maître : un joueur ne peut utiliser une capacité (Ten, Zetsu, Ren…) qu'après
 * l'avoir apprise, en général par une quête de Wing (récompense {@code reward_learn}). Règle
 * {@code hxhNenLearning} (activée par défaut) ; désactivée, tout est su d'office. Les mobs savent tout.
 */
public final class NenLearning {
    /** Capacités qui s'apprennent. */
    public enum Ability {
        TEN, IN, ZETSU, REN, KEN, GYO, EN, KO, RYU, SHU, HATSU;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String translationKey() {
            return "ability.hxh." + id();
        }

        @Nullable
        public static Ability byId(String id) {
            for (Ability a : values()) if (a.id().equals(id)) return a;
            return null;
        }
    }

    /** Les GameTests jouent sans apprentissage, sauf ceux qui le testent. */
    public static boolean forceInTests = false;

    private NenLearning() {}

    /** L'apprentissage s'applique à cette entité (joueur, règle active). */
    public static boolean applies(LivingEntity entity) {
        if (!(entity instanceof Player) || !(entity.level() instanceof ServerLevel level)) return false;
        if (level.getServer() instanceof net.minecraft.gametest.framework.GameTestServer && !forceInTests) return false;
        return level.getGameRules().getBoolean(HxHGameRules.NEN_LEARNING);
    }

    public static boolean knows(LivingEntity entity, Ability ability) {
        return !applies(entity) || HunterStats.get(entity).learned(ability.id());
    }

    /** Clé de refus si la capacité n'est pas apprise, sinon null. */
    @Nullable
    public static String refusal(LivingEntity entity, Ability ability) {
        return knows(entity, ability) ? null : "message.hxh.not_learned." + ability.id();
    }

    /** Capacité nécessaire pour une posture (null pour le neutre). */
    @Nullable
    public static Ability forMode(NenMode mode) {
        return switch (mode) {
            case TEN -> Ability.TEN;
            case IN -> Ability.IN;
            case ZETSU -> Ability.ZETSU;
            case REN -> Ability.REN;
            case KEN -> Ability.KEN;
            case NONE -> null;
        };
    }

    public static void learnAll(LivingEntity entity) {
        HunterData d = HunterStats.get(entity);
        for (Ability a : Ability.values()) d.learn(a.id());
    }
}
