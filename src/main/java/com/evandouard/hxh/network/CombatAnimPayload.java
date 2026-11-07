package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

/** Serveur → client : une entité joue une animation de combat ({@link com.evandouard.hxh.combat.CombatAnim}). */
public record CombatAnimPayload(int entityId, int anim) implements CustomPacketPayload {
    public static final Type<CombatAnimPayload> TYPE = new Type<>(HxHMod.id("combat_anim"));

    public static final StreamCodec<ByteBuf, CombatAnimPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CombatAnimPayload::entityId,
            ByteBufCodecs.VAR_INT, CombatAnimPayload::anim,
            CombatAnimPayload::new);

    /** Rempli par le client au démarrage (évite de charger des classes client sur un serveur dédié). */
    public static Consumer<CombatAnimPayload> clientHandler = p -> {};

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CombatAnimPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientHandler.accept(payload));
    }
}
