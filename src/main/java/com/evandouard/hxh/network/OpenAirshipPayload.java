package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Serveur → client : ouvre la carte du dirigeable (Jenny du joueur et destinations avec leur prix). */
public record OpenAirshipPayload(int jenny, List<Dest> destinations, boolean darkContinentKnown) implements CustomPacketPayload {
    public record Dest(String id, int x, int z, int price) {
        public static final StreamCodec<ByteBuf, Dest> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Dest::id,
                ByteBufCodecs.VAR_INT, Dest::x,
                ByteBufCodecs.VAR_INT, Dest::z,
                ByteBufCodecs.VAR_INT, Dest::price,
                Dest::new);
    }

    public static final Type<OpenAirshipPayload> TYPE = new Type<>(HxHMod.id("open_airship"));
    public static final StreamCodec<ByteBuf, OpenAirshipPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenAirshipPayload::jenny,
            Dest.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenAirshipPayload::destinations,
            ByteBufCodecs.BOOL, OpenAirshipPayload::darkContinentKnown,
            OpenAirshipPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenAirshipPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.evandouard.hxh.client.ClientHooks.openAirship(payload));
    }
}
