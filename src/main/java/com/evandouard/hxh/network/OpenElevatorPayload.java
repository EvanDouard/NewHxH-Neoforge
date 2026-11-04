package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur → client : ouvrir l'écran de l'ascenseur (étage maximum autorisé, étage actuel). */
public record OpenElevatorPayload(int maxFloor, int currentFloor) implements CustomPacketPayload {
    public static final Type<OpenElevatorPayload> TYPE = new Type<>(HxHMod.id("open_elevator"));

    public static final StreamCodec<ByteBuf, OpenElevatorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenElevatorPayload::maxFloor,
            ByteBufCodecs.VAR_INT, OpenElevatorPayload::currentFloor,
            OpenElevatorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenElevatorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.evandouard.hxh.client.ClientHooks.openElevator(payload.maxFloor(), payload.currentFloor()));
    }
}
