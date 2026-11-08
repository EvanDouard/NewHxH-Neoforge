package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.stats.HunterStats;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → serveur : part du bonus de Vitesse que le joueur veut utiliser (réglage du carnet, 0 à 1). */
public record SpeedCapPayload(float cap) implements CustomPacketPayload {
    public static final Type<SpeedCapPayload> TYPE = new Type<>(HxHMod.id("speed_cap"));
    public static final StreamCodec<ByteBuf, SpeedCapPayload> STREAM_CODEC =
            ByteBufCodecs.FLOAT.map(SpeedCapPayload::new, SpeedCapPayload::cap);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpeedCapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            HunterStats.get(context.player()).setSpeedCap(payload.cap());
            HunterStats.applyAttributes(context.player());
        });
    }
}
