package com.evandouard.hxh.network;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class HxHNetwork {
    private HxHNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SyncHunterPayload.TYPE, SyncHunterPayload.STREAM_CODEC, SyncHunterPayload::handle);
        registrar.playToClient(QuestStatusPayload.TYPE, QuestStatusPayload.STREAM_CODEC, QuestStatusPayload::handle);
        registrar.playToClient(DamageNumberPayload.TYPE, DamageNumberPayload.STREAM_CODEC, DamageNumberPayload::handle);
        registrar.playToServer(TrackQuestPayload.TYPE, TrackQuestPayload.STREAM_CODEC, TrackQuestPayload::handle);
        registrar.playToClient(com.evandouard.hxh.fx.NenFx.Payload.TYPE, com.evandouard.hxh.fx.NenFx.Payload.STREAM_CODEC, com.evandouard.hxh.fx.NenFx.Payload::handle);
        registrar.playToServer(SpeedCapPayload.TYPE, SpeedCapPayload.STREAM_CODEC, SpeedCapPayload::handle);
        registrar.playToClient(HudPayload.TYPE, HudPayload.STREAM_CODEC, HudPayload::handle);
        registrar.playToClient(ProfilePayload.TYPE, ProfilePayload.STREAM_CODEC, ProfilePayload::handle);
        registrar.playToServer(RequestProfilePayload.TYPE, RequestProfilePayload.STREAM_CODEC, RequestProfilePayload::handle);
        registrar.playToClient(OpenElevatorPayload.TYPE, OpenElevatorPayload.STREAM_CODEC, OpenElevatorPayload::handle);
        registrar.playToServer(ElevatorGoPayload.TYPE, ElevatorGoPayload.STREAM_CODEC, ElevatorGoPayload::handle);
        registrar.playToServer(SetRyuPayload.TYPE, SetRyuPayload.STREAM_CODEC, SetRyuPayload::handle);
        registrar.playToServer(VowPayload.TYPE, VowPayload.STREAM_CODEC, VowPayload::handle);
        registrar.playToServer(ChooseKitPayload.TYPE, ChooseKitPayload.STREAM_CODEC, ChooseKitPayload::handle);
        registrar.playToServer(NenActionPayload.TYPE, NenActionPayload.STREAM_CODEC, NenActionPayload::handle);
        registrar.playToClient(CombatAnimPayload.TYPE, CombatAnimPayload.STREAM_CODEC, CombatAnimPayload::handle);
        registrar.playToServer(PunchPayload.TYPE, PunchPayload.STREAM_CODEC, PunchPayload::handle);
        registrar.playToServer(ToggleNenModePayload.TYPE, ToggleNenModePayload.STREAM_CODEC, ToggleNenModePayload::handle);
        registrar.playToClient(OpenShopPayload.TYPE, OpenShopPayload.STREAM_CODEC, OpenShopPayload::handle);
        registrar.playToClient(OpenAuctionPayload.TYPE, OpenAuctionPayload.STREAM_CODEC, OpenAuctionPayload::handle);
        registrar.playToClient(ArmWrestleStatePayload.TYPE, ArmWrestleStatePayload.STREAM_CODEC, ArmWrestleStatePayload::handle);
        registrar.playToServer(ArmWrestleInputPayload.TYPE, ArmWrestleInputPayload.STREAM_CODEC, ArmWrestleInputPayload::handle);
        registrar.playToServer(AuctionBidPayload.TYPE, AuctionBidPayload.STREAM_CODEC, AuctionBidPayload::handle);
        registrar.playToClient(OpenComputerPayload.TYPE, OpenComputerPayload.STREAM_CODEC, OpenComputerPayload::handle);
        registrar.playToServer(ShopActionPayload.TYPE, ShopActionPayload.STREAM_CODEC, ShopActionPayload::handle);
    }

    /**
     * Envoi sûr à un joueur : ignoré si sa connexion ne connaît pas nos paquets
     * (joueurs factices des tests ou d'autres mods).
     */
    public static void sendTo(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload.type())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /** Envoi sûr à tous les joueurs qui voient l'entité, et à elle-même si c'est un joueur. */
    public static void sendToTrackingAndSelf(net.minecraft.world.entity.Entity entity,
                                             net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        java.util.Set<ServerPlayer> targets = new java.util.HashSet<>(
                level.getChunkSource().chunkMap.getPlayers(entity.chunkPosition(), false));
        if (entity instanceof ServerPlayer self) targets.add(self);
        for (ServerPlayer p : targets) sendTo(p, payload);
    }

    /** Envoie toutes les données Hunter d'un joueur à lui-même. */
    public static void syncFull(ServerPlayer player) {
        HunterData data = player.getData(ModAttachments.HUNTER);
        data.writeDirtyAndClear(); // ce qui était en attente part dans l'envoi complet
        sendTo(player, new SyncHunterPayload(player.getId(), data.writeAll()));
    }

    /** Envoie toutes les données Hunter d'une entité à un joueur qui commence à la voir. */
    public static void syncTo(ServerPlayer viewer, LivingEntity entity) {
        if (!entity.hasData(ModAttachments.HUNTER)) return;
        sendTo(viewer, new SyncHunterPayload(entity.getId(), entity.getData(ModAttachments.HUNTER).writeAll()));
    }

    /** Envoie seulement les champs modifiés, au joueur et à ceux qui le voient. */
    public static void syncDirty(LivingEntity entity) {
        if (!entity.hasData(ModAttachments.HUNTER)) return;
        HunterData data = entity.getData(ModAttachments.HUNTER);
        if (!data.isDirty()) return;
        sendToTrackingAndSelf(entity, new SyncHunterPayload(entity.getId(), data.writeDirtyAndClear()));
    }
}
