package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.auction.Auction;
import com.evandouard.hxh.auction.Auctions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → serveur : enchérir sur le lot n° {@code slot} du jour. Le commissaire-priseur doit être tout près. */
public record AuctionBidPayload(String auction, int slot) implements CustomPacketPayload {
    public static final Type<AuctionBidPayload> TYPE = new Type<>(HxHMod.id("auction_bid"));
    public static final StreamCodec<ByteBuf, AuctionBidPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AuctionBidPayload::auction,
            ByteBufCodecs.VAR_INT, AuctionBidPayload::slot,
            AuctionBidPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AuctionBidPayload p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ResourceLocation id = ResourceLocation.tryParse(p.auction());
            Auction a = id == null ? null : Auctions.get(id);
            if (a == null) return;
            boolean near = !player.level().getEntities(com.evandouard.hxh.registry.ModEntities.NPC.get(),
                    player.getBoundingBox().inflate(ShopActionPayload.REACH), n -> n.npcId().equals(a.npc())).isEmpty();
            if (!near) return;
            Auctions.bid(player, a, p.slot());
            Auctions.open(player, a);
        });
    }
}
