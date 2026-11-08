package com.evandouard.hxh.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Côté serveur : notifications et gros lettrages de l'interface (sans effet pour un mob). */
public final class Hud {
    public static final int GOLD = 0xFFFFD23F, RED = 0xFFFF5A3C, BLUE = 0xFF3CC8FF, GREEN = 0xFF52D273, PURPLE = 0xFFB06BFF;

    private Hud() {}

    /** Notification en haut à gauche : badge (« +1 », « ! »), titre, sous-titre. */
    public static void toast(LivingEntity entity, Component title, Component subtitle, String badge, int color) {
        if (entity instanceof ServerPlayer player) {
            HxHNetwork.sendTo(player, new HudPayload(HudPayload.TOAST, title, subtitle, badge, color));
        }
    }

    /** Gros lettrage au centre (« FRAPPE PARFAITE ! »). */
    public static void callout(LivingEntity entity, Component text, int color, boolean big) {
        if (entity instanceof ServerPlayer player) {
            HxHNetwork.sendTo(player, new HudPayload(big ? HudPayload.CALLOUT : HudPayload.CALLOUT_SMALL,
                    text, Component.empty(), "", color));
        }
    }

    /** Couleur associée à une stat (même code que le carnet). */
    public static int statColor(com.evandouard.hxh.data.Stat stat) {
        return switch (stat) {
            case STRENGTH -> RED;
            case SPEED -> BLUE;
            case ENDURANCE -> 0xFFB0B0B0;
            case VITALITY -> GREEN;
            case WILLPOWER -> PURPLE;
        };
    }
}
