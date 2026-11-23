package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → serveur : touche de saut pressée en l'air. Simple intention ; le serveur décide s'il y a un saut en plus (Meneur). */
public record AirJumpPayload() implements CustomPacketPayload {
    public static final AirJumpPayload INSTANCE = new AirJumpPayload();
    public static final Type<AirJumpPayload> TYPE = new Type<>(HxHMod.id("air_jump"));
    public static final StreamCodec<ByteBuf, AirJumpPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AirJumpPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.evandouard.hxh.hatsu.Characters.airJump(context.player()));
    }
}
