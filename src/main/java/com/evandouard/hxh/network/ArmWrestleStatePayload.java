package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur → client : état de la manche de bras de fer (repère, zone, progression, résultat 0/1/2). */
public record ArmWrestleStatePayload(float marker, float zoneCenter, float zoneSize, float progress, int result)
        implements CustomPacketPayload {
    public static final Type<ArmWrestleStatePayload> TYPE = new Type<>(HxHMod.id("armwrestle_state"));
    public static final StreamCodec<ByteBuf, ArmWrestleStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ArmWrestleStatePayload::marker,
            ByteBufCodecs.FLOAT, ArmWrestleStatePayload::zoneCenter,
            ByteBufCodecs.FLOAT, ArmWrestleStatePayload::zoneSize,
            ByteBufCodecs.FLOAT, ArmWrestleStatePayload::progress,
            ByteBufCodecs.VAR_INT, ArmWrestleStatePayload::result,
            ArmWrestleStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ArmWrestleStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.evandouard.hxh.client.ClientHooks.armWrestle(payload));
    }
}
