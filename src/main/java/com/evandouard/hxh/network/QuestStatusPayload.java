package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Serveur → client : quête suivie à afficher (titre vide = aucune). Les textes arrivent déjà prêts
 * (clé de traduction avec, au besoin, le texte écrit dans le JSON de la quête comme repli).
 */
public record QuestStatusPayload(Component title, Component step, int progress, int goal) implements CustomPacketPayload {
    public static final Type<QuestStatusPayload> TYPE = new Type<>(HxHMod.id("quest_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.TRUSTED_STREAM_CODEC, QuestStatusPayload::title,
            ComponentSerialization.TRUSTED_STREAM_CODEC, QuestStatusPayload::step,
            ByteBufCodecs.VAR_INT, QuestStatusPayload::progress,
            ByteBufCodecs.VAR_INT, QuestStatusPayload::goal,
            QuestStatusPayload::new);

    public static final QuestStatusPayload NONE = new QuestStatusPayload(Component.empty(), Component.empty(), 0, 0);

    /** Dernier état reçu (lu par l'interface en jeu). */
    public static volatile QuestStatusPayload latest = NONE;

    public boolean isEmpty() {
        return title.getString().isEmpty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> latest = payload);
    }
}
