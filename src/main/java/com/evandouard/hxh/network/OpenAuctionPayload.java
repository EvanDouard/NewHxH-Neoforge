package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Serveur → client : lots du jour d'une vente aux enchères et Jenny du joueur (ouvre ou rafraîchit l'écran). */
public record OpenAuctionPayload(String auction, String npc, int jenny, List<Line> lots) implements CustomPacketPayload {
    /** Un lot : l'objet, le prix actuel, la prochaine mise du joueur, le statut (voir Auctions). */
    public record Line(ItemStack stack, int price, int nextBid, int status) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Line> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Line::stack,
                ByteBufCodecs.VAR_INT, Line::price,
                ByteBufCodecs.VAR_INT, Line::nextBid,
                ByteBufCodecs.VAR_INT, Line::status,
                Line::new);
    }

    public static final Type<OpenAuctionPayload> TYPE = new Type<>(HxHMod.id("open_auction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAuctionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenAuctionPayload::auction,
            ByteBufCodecs.STRING_UTF8, OpenAuctionPayload::npc,
            ByteBufCodecs.VAR_INT, OpenAuctionPayload::jenny,
            Line.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenAuctionPayload::lots,
            OpenAuctionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenAuctionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.evandouard.hxh.client.ClientHooks.openAuction(payload));
    }
}
