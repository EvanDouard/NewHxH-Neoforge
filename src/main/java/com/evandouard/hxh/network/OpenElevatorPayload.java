package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Serveur → client : ouvrir l'ascenseur.
 * @param brackets tranches accessibles (« 1–50 », « 51–100 »…), de la plus basse à celle du joueur
 * @param current lieu actuel (−1 chambre, 0 hall d'entrée, n tranche n − 1)
 */
public record OpenElevatorPayload(List<String> brackets, int current, boolean room) implements CustomPacketPayload {
    public static final Type<OpenElevatorPayload> TYPE = new Type<>(HxHMod.id("open_elevator"));

    public static final StreamCodec<ByteBuf, OpenElevatorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenElevatorPayload::brackets,
            ByteBufCodecs.VAR_INT, OpenElevatorPayload::current,
            ByteBufCodecs.BOOL, OpenElevatorPayload::room,
            OpenElevatorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenElevatorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.evandouard.hxh.client.ClientHooks.openElevator(payload));
    }
}
