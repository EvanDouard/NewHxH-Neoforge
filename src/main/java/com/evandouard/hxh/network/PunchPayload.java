package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → serveur : le joueur a frappé dans le vide (clic gauche sans cible). Simple intention :
 * le serveur décide s'il anime un coup de poing (mains nues, pas trop rapproché du précédent).
 */
public record PunchPayload() implements CustomPacketPayload {
    public static final PunchPayload INSTANCE = new PunchPayload();
    public static final Type<PunchPayload> TYPE = new Type<>(HxHMod.id("punch"));
    public static final StreamCodec<ByteBuf, PunchPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PunchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.evandouard.hxh.combat.CombatAnimations.punch(context.player()));
    }
}
