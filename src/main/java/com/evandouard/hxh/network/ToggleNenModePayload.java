package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.nen.NenModes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → serveur : le joueur a pressé la touche d'un mode (Ten, Zetsu, Ren).
 * Ce n'est qu'une intention : le serveur décide.
 */
public record ToggleNenModePayload(String mode) implements CustomPacketPayload {
    public static final Type<ToggleNenModePayload> TYPE = new Type<>(HxHMod.id("toggle_nen_mode"));

    public static final StreamCodec<ByteBuf, ToggleNenModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ToggleNenModePayload::mode,
            ToggleNenModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleNenModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NenMode mode = NenMode.byId(payload.mode());
            if (mode != null) NenModes.toggle(context.player(), mode);
        });
    }
}
