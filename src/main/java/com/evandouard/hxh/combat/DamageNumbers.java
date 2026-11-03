package com.evandouard.hxh.combat;

import com.evandouard.hxh.network.DamageNumberPayload;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/**
 * Chiffres flottants : « -10 » en rouge quand une entité prend des dégâts, « +1 » en vert quand elle se soigne.
 * Le serveur envoie l'info aux joueurs qui voient l'entité ; le client les affiche au-dessus d'elle.
 */
public final class DamageNumbers {
    private DamageNumbers() {}

    public static void send(LivingEntity entity, float amount, boolean heal) {
        if (entity.level().isClientSide() || amount < 0.05f) return;
        com.evandouard.hxh.network.HxHNetwork.sendToTrackingAndSelf(entity, new DamageNumberPayload(entity.getId(), amount, heal));
    }

    /** « -10 », « -2.5 », « +1 » : entier au-dessus de 10, sinon une décimale (sans « .0 »). */
    public static String format(float amount, boolean heal) {
        String sign = heal ? "+" : "-";
        if (amount >= 10) return sign + Math.round(amount);
        float rounded = Math.round(amount * 10) / 10f;
        String n = rounded == (int) rounded ? Integer.toString((int) rounded) : String.format(Locale.ROOT, "%.1f", rounded);
        return sign + n;
    }
}
