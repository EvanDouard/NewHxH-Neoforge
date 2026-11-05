package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.stats.ProfileText;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Serveur → client : contenu des onglets du carnet de Hunter. */
public record ProfilePayload(List<Component> character, List<Component> quests, List<Component> arena)
        implements CustomPacketPayload {
    public static final Type<ProfilePayload> TYPE = new Type<>(HxHMod.id("profile"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Component>> LINES =
            ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, ProfilePayload> STREAM_CODEC = StreamCodec.composite(
            LINES, ProfilePayload::character,
            LINES, ProfilePayload::quests,
            LINES, ProfilePayload::arena,
            ProfilePayload::new);

    public static ProfilePayload of(ServerPlayer player) {
        return new ProfilePayload(ProfileText.statsAndNen(player), ProfileText.quests(player), ProfileText.arena(player));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ProfilePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.evandouard.hxh.client.ClientHooks.openProfile(payload));
    }
}
