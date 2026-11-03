package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.nen.NenAwakening;
import com.evandouard.hxh.nen.NenHealing;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → serveur : intention d'action liée au Nen (touche pressée ou relâchée).
 */
public record NenActionPayload(Action action, boolean pressed) implements CustomPacketPayload {
    public enum Action { HEAL, MEDITATE, OPEN_PORES, CAST, NEXT_TECHNIQUE, KO, GYO, EN }

    public static final Type<NenActionPayload> TYPE = new Type<>(HxHMod.id("nen_action"));

    public static final StreamCodec<ByteBuf, NenActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> Action.values()[Math.floorMod(i, Action.values().length)], Action::ordinal),
            NenActionPayload::action,
            ByteBufCodecs.BOOL, NenActionPayload::pressed,
            NenActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NenActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            switch (payload.action()) {
                case HEAL -> NenHealing.request(context.player(), payload.pressed());
                case MEDITATE -> { if (payload.pressed()) NenAwakening.toggleMeditation(context.player()); }
                case OPEN_PORES -> { if (payload.pressed()) NenAwakening.requestOpenPores(context.player()); }
                case CAST -> { if (payload.pressed()) Hatsu.requestCast(context.player()); }
                case NEXT_TECHNIQUE -> { if (payload.pressed()) Hatsu.cycle(context.player()); }
                case GYO -> { if (payload.pressed()) com.evandouard.hxh.nen.NenSenses.toggleGyo(context.player()); }
                case EN -> { if (payload.pressed()) com.evandouard.hxh.nen.NenSenses.toggleEn(context.player()); }
                case KO -> { if (payload.pressed()) com.evandouard.hxh.nen.NenDefense.requestKo(context.player()); }
            }
        });
    }
}
