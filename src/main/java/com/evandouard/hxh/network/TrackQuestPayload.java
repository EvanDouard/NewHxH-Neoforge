package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.quest.QuestManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → serveur : suivre une autre quête en cours (bouton du carnet). Le serveur renvoie le carnet à jour. */
public record TrackQuestPayload(String questId) implements CustomPacketPayload {
    public static final Type<TrackQuestPayload> TYPE = new Type<>(HxHMod.id("track_quest"));
    public static final StreamCodec<ByteBuf, TrackQuestPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(TrackQuestPayload::new, TrackQuestPayload::questId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TrackQuestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer p)) return;
            if (QuestManager.data(p).track(payload.questId())) QuestManager.sync(p);
            HxHNetwork.sendTo(p, ProfilePayload.of(p));
        });
    }
}
