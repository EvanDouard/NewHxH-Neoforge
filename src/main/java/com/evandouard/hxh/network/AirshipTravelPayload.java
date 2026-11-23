package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.airship.Airship;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → serveur : le joueur a confirmé un voyage en dirigeable. Simple intention ; le serveur vérifie borne, prix et destination. */
public record AirshipTravelPayload(String destination) implements CustomPacketPayload {
    public static final Type<AirshipTravelPayload> TYPE = new Type<>(HxHMod.id("airship_travel"));
    public static final StreamCodec<ByteBuf, AirshipTravelPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AirshipTravelPayload::destination,
            AirshipTravelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AirshipTravelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            String refusal = Airship.travel(player, payload.destination(), true);
            if (refusal != null) player.sendSystemMessage(Component.translatable(refusal).withStyle(ChatFormatting.RED));
        });
    }
}
