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
public record ProfilePayload(List<Component> character, List<Component> quests, List<Component> arena,
                             List<String> runningQuests, String trackedQuest, Extra extra)
        implements CustomPacketPayload {
    /**
     * Graphe du Nen, détail par niveau (stats, techniques, aura, soin) et textes des fiches du personnage
     * (identité Nen, famille). Regroupés pour garder un codec à six champs.
     */
    public record Extra(List<String> graph, List<String> details, List<Component> identity, List<Component> family) {
        public static final Extra EMPTY = new Extra(List.of(), List.of(), List.of(), List.of());
    }

    public List<String> graph() {
        return extra.graph();
    }

    public List<String> details() {
        return extra.details();
    }

    public static final Type<ProfilePayload> TYPE = new Type<>(HxHMod.id("profile"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Component>> LINES =
            ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list());

    private static final StreamCodec<RegistryFriendlyByteBuf, Extra> EXTRA = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Extra::graph,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(4096)), Extra::details,
            LINES, Extra::identity,
            LINES, Extra::family,
            Extra::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, ProfilePayload> STREAM_CODEC = StreamCodec.composite(
            LINES, ProfilePayload::character,
            LINES, ProfilePayload::quests,
            LINES, ProfilePayload::arena,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ProfilePayload::runningQuests,
            ByteBufCodecs.STRING_UTF8, ProfilePayload::trackedQuest,
            EXTRA, ProfilePayload::extra,
            ProfilePayload::new);

    public static ProfilePayload of(ServerPlayer player) {
        var d = com.evandouard.hxh.quest.QuestManager.data(player);
        var extra = new Extra(com.evandouard.hxh.nen.NenTree.encode(player), com.evandouard.hxh.stats.ProfileDetails.encode(player),
                ProfileText.identity(player), ProfileText.family(player));
        return new ProfilePayload(ProfileText.summary(player), ProfileText.quests(player), ProfileText.arena(player),
                d.runningQuests(), d.active(), extra);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ProfilePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.evandouard.hxh.client.ClientHooks.openProfile(payload));
    }
}
