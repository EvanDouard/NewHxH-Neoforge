package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

/**
 * Serveur → client : un élément d'interface à afficher.
 * <ul>
 *     <li>{@link #TOAST} : notification qui glisse en haut à gauche (niveau gagné, nouvelle étape…) ;</li>
 *     <li>{@link #CALLOUT} / {@link #CALLOUT_SMALL} : gros lettrage au centre de l'écran (frappe parfaite, combo…).</li>
 * </ul>
 */
public record HudPayload(int kind, Component title, Component subtitle, String badge, int color) implements CustomPacketPayload {
    public static final int TOAST = 0, CALLOUT = 1, CALLOUT_SMALL = 2;

    public static final Type<HudPayload> TYPE = new Type<>(HxHMod.id("hud"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HudPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HudPayload::kind,
            ComponentSerialization.TRUSTED_STREAM_CODEC, HudPayload::title,
            ComponentSerialization.TRUSTED_STREAM_CODEC, HudPayload::subtitle,
            ByteBufCodecs.STRING_UTF8, HudPayload::badge,
            ByteBufCodecs.INT, HudPayload::color,
            HudPayload::new);

    /** Rempli par le client au démarrage. */
    public static Consumer<HudPayload> clientHandler = p -> {};

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HudPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientHandler.accept(payload));
    }
}
