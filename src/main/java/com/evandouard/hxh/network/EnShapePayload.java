package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.nen.NenSenses;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → serveur : choisir la forme de l'En (le serveur vérifie la maîtrise et que la forme est faisable). */
public record EnShapePayload(String shape, float a, float b) implements CustomPacketPayload {
    public static final Type<EnShapePayload> TYPE = new Type<>(HxHMod.id("en_shape"));

    public static final StreamCodec<ByteBuf, EnShapePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EnShapePayload::shape,
            ByteBufCodecs.FLOAT, EnShapePayload::a,
            ByteBufCodecs.FLOAT, EnShapePayload::b,
            EnShapePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EnShapePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            String refusal = NenSenses.chooseEnShape(player, payload.shape(), payload.a(), payload.b());
            player.displayClientMessage(Component.translatable(refusal != null ? refusal : "message.hxh.en_shape_set"), true);
        });
    }
}
