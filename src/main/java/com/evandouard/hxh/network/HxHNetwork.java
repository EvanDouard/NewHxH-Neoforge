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
        registrar.playToServer(SetRyuPayload.TYPE, SetRyuPayload.STREAM_CODEC, SetRyuPayload::handle);
        registrar.playToServer(ChooseKitPayload.TYPE, ChooseKitPayload.STREAM_CODEC, ChooseKitPayload::handle);
        registrar.playToServer(NenActionPayload.TYPE, NenActionPayload.STREAM_CODEC, NenActionPayload::handle);
        registrar.playToServer(ToggleNenModePayload.TYPE, ToggleNenModePayload.STREAM_CODEC, ToggleNenModePayload::handle);
    }

    /** Envoie toutes les données Hunter d'un joueur à lui-même. */
    public static void syncFull(ServerPlayer player) {
        HunterData data = player.getData(ModAttachments.HUNTER);
        data.writeDirtyAndClear(); // ce qui était en attente part dans l'envoi complet
        PacketDistributor.sendToPlayer(player, new SyncHunterPayload(player.getId(), data.writeAll()));
    }

    /** Envoie toutes les données Hunter d'une entité à un joueur qui commence à la voir. */
    public static void syncTo(ServerPlayer viewer, LivingEntity entity) {
        if (!entity.hasData(ModAttachments.HUNTER)) return;
        PacketDistributor.sendToPlayer(viewer,
                new SyncHunterPayload(entity.getId(), entity.getData(ModAttachments.HUNTER).writeAll()));
    }

    /** Envoie seulement les champs modifiés, au joueur et à ceux qui le voient. */
    public static void syncDirty(LivingEntity entity) {
        if (!entity.hasData(ModAttachments.HUNTER)) return;
        HunterData data = entity.getData(ModAttachments.HUNTER);
        if (!data.isDirty()) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity,
                new SyncHunterPayload(entity.getId(), data.writeDirtyAndClear()));
    }
}
