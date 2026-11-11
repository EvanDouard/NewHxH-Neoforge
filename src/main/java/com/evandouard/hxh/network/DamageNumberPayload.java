package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

/** Serveur → client : une entité vient de prendre des dégâts (ou d'être soignée). */
/**
 * {@code amount} = dégâts physiques (Force) ou soin ; {@code nen} = dégâts du Nen (affichés en bleu à côté) ;
 * {@code blocked} = ce que la défense de la cible a encaissé (Endurance, Ten, Ken…).
 */
public record DamageNumberPayload(int entityId, float amount, boolean heal, float nen, float blocked) implements CustomPacketPayload {
    public static final Type<DamageNumberPayload> TYPE = new Type<>(HxHMod.id("damage_number"));

    public static final StreamCodec<ByteBuf, DamageNumberPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DamageNumberPayload::entityId,
            ByteBufCodecs.FLOAT, DamageNumberPayload::amount,
            ByteBufCodecs.BOOL, DamageNumberPayload::heal,
            ByteBufCodecs.FLOAT, DamageNumberPayload::nen,
            ByteBufCodecs.FLOAT, DamageNumberPayload::blocked,
            DamageNumberPayload::new);

    /** Rempli par le client au démarrage (évite de charger des classes client sur un serveur dédié). */
    public static Consumer<DamageNumberPayload> clientHandler = p -> {};

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DamageNumberPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientHandler.accept(payload));
    }
}
