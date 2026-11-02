package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.hatsu.Hatsu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → serveur : le joueur a confirmé le choix de son personnage (Hatsu définitif).
 * Le serveur revérifie tout (type de Nen, divination faite, pas de choix précédent).
 */
public record ChooseKitPayload(String kitId) implements CustomPacketPayload {
    public static final Type<ChooseKitPayload> TYPE = new Type<>(HxHMod.id("choose_kit"));

    public static final StreamCodec<ByteBuf, ChooseKitPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ChooseKitPayload::kitId,
            ChooseKitPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChooseKitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Hatsu.requestChoose(context.player(), payload.kitId()));
    }
}
