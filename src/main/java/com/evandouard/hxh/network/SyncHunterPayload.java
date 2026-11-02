package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Serveur → client : données Hunter d'une entité. {@code data} ne contient que les clés modifiées
 * (ou toutes à la connexion / réapparition).
 */
public record SyncHunterPayload(int entityId, CompoundTag data) implements CustomPacketPayload {
    public static final Type<SyncHunterPayload> TYPE = new Type<>(HxHMod.id("sync_hunter"));

    public static final StreamCodec<ByteBuf, SyncHunterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncHunterPayload::entityId,
            ByteBufCodecs.COMPOUND_TAG, SyncHunterPayload::data,
            SyncHunterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Appliqué côté client. N'utilise que des classes communes (sûr sur serveur dédié). */
    public static void handle(SyncHunterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = context.player().level().getEntity(payload.entityId());
            if (entity instanceof LivingEntity living) {
                living.getData(ModAttachments.HUNTER).read(payload.data());
            }
        });
    }
}
