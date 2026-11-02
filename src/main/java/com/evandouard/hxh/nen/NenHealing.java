package com.evandouard.hxh.nen;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Soin par le Nen : on convertit de l'aura en vie tant que la touche est maintenue.
 * Plus la maîtrise monte, plus le soin est rapide et moins il coûte.
 */
public final class NenHealing {
    private NenHealing() {}

    public static double healPerSecond(int level) {
        return HxHConfig.HEAL_BASE_PER_SECOND.get() + level * HxHConfig.HEAL_PER_LEVEL.get();
    }

    public static double auraPerHealth(int level) {
        return Math.max(HxHConfig.HEAL_MIN_COST.get(),
                HxHConfig.HEAL_BASE_COST.get() - level * HxHConfig.HEAL_COST_REDUCTION_PER_LEVEL.get());
    }

    /**
     * Démarre ou arrête le soin.
     * @return null si accepté, sinon la clé de traduction du refus
     */
    public static String setHealing(LivingEntity entity, boolean active) {
        HunterData data = HunterStats.get(entity);
        if (!active) {
            data.setHealing(false);
            return null;
        }
        if (!data.nenUnlocked()) return "message.hxh.nen_locked";
        if (!data.healUnlocked()) return "message.hxh.heal_locked";
        if (data.mode() == NenMode.ZETSU) return "message.hxh.heal_zetsu";
        data.setHealing(true);
        return null;
    }

    public static void request(LivingEntity entity, boolean active) {
        String refusal = setHealing(entity, active);
        if (refusal != null && entity instanceof Player player) {
            player.displayClientMessage(Component.translatable(refusal), true);
        }
    }

    /** Appelé chaque tick serveur. */
    public static void tick(LivingEntity entity) {
        if (!entity.hasData(ModAttachments.HUNTER)) return;
        HunterData data = HunterStats.get(entity);
        if (!data.healing()) return;
        if (!data.nenUnlocked() || !data.healUnlocked() || data.mode() == NenMode.ZETSU) {
            data.setHealing(false);
            return;
        }
        float missing = entity.getMaxHealth() - entity.getHealth();
        if (missing <= 0) return; // vie pleine : on garde la touche, mais rien ne se consomme

        int level = data.healLevel();
        float amount = (float) Math.min(healPerSecond(level) / 20.0, missing);
        float cost = (float) (amount * auraPerHealth(level));
        if (data.aura() < cost) {
            data.setHealing(false);
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.hxh.no_aura"), true);
            }
            return;
        }
        data.setAura(data.aura() - cost);
        entity.heal(amount);

        // Maîtrise : XP chaque seconde de soin effectif, un peu plus vite à haut niveau
        if (entity.tickCount % 20 == 0) {
            int gained = data.addHealXp(2 + level / 10);
            if (gained > 0 && entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.hxh.heal_level_up", data.healLevel()), true);
            }
        }
    }
}
