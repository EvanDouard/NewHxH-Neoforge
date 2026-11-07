package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur → client : quête en cours à afficher (titre vide = aucune ; stepArg = argument de l'objectif). */
public record QuestStatusPayload(String titleKey, String stepKey, String stepArg, int progress, int goal) implements CustomPacketPayload {
    public static final Type<QuestStatusPayload> TYPE = new Type<>(HxHMod.id("quest_status"));

    public static final StreamCodec<ByteBuf, QuestStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, QuestStatusPayload::titleKey,
            ByteBufCodecs.STRING_UTF8, QuestStatusPayload::stepKey,
            ByteBufCodecs.STRING_UTF8, QuestStatusPayload::stepArg,
            ByteBufCodecs.VAR_INT, QuestStatusPayload::progress,
            ByteBufCodecs.VAR_INT, QuestStatusPayload::goal,
            QuestStatusPayload::new);

    /** Dernier état reçu (lu par le HUD côté client). */
    public static volatile QuestStatusPayload latest = new QuestStatusPayload("", "", "", 0, 0);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> latest = payload);
    }
}
