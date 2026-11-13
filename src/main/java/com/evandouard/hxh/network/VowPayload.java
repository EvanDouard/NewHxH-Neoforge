package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.hatsu.Vows;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → serveur : prêter un serment ({@code take}, id) ou y renoncer ({@code renounce}). */
public record VowPayload(String action, String vowId) implements CustomPacketPayload {
    public static final Type<VowPayload> TYPE = new Type<>(HxHMod.id("vow"));

    public static final StreamCodec<ByteBuf, VowPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, VowPayload::action,
            ByteBufCodecs.STRING_UTF8, VowPayload::vowId,
            VowPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VowPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if ("toggle".equals(payload.action())) {
                player.displayClientMessage(Component.translatable(Vows.toggle(player)), true);
                return;
            }
            if ("renounce".equals(payload.action())) {
                Vows.breakVow(player, "vow.hxh.reason.renounced");
                return;
            }
            String refusal = Vows.take(player, payload.vowId());
            if (refusal != null) player.displayClientMessage(Component.translatable(refusal), true);
        });
    }
}
