package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → serveur : bras de fer, le joueur tient (true) ou lâche (false). */
public record ArmWrestleInputPayload(boolean holding) implements CustomPacketPayload {
    public static final Type<ArmWrestleInputPayload> TYPE = new Type<>(HxHMod.id("armwrestle_input"));
    public static final StreamCodec<ByteBuf, ArmWrestleInputPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ArmWrestleInputPayload::holding, ArmWrestleInputPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ArmWrestleInputPayload p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) com.evandouard.hxh.minigame.ArmWrestle.input(sp, p.holding());
        });
    }
}
