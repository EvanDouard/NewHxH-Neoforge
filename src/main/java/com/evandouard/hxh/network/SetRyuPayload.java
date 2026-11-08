package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.stats.HunterStats;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → serveur : nouvelle répartition du Ryu (part de l'aura dans l'attaque, en %). */
public record SetRyuPayload(int attackPercent) implements CustomPacketPayload {
    public static final Type<SetRyuPayload> TYPE = new Type<>(HxHMod.id("set_ryu"));

    public static final StreamCodec<ByteBuf, SetRyuPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetRyuPayload::attackPercent,
            SetRyuPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetRyuPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            String refusal = com.evandouard.hxh.nen.NenLearning.refusal(context.player(), com.evandouard.hxh.nen.NenLearning.Ability.RYU);
            if (refusal != null) {
                context.player().displayClientMessage(net.minecraft.network.chat.Component.translatable(refusal), true);
                return;
            }
            HunterStats.get(context.player()).setRyuAttack(payload.attackPercent());
        });
    }
}
