package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Serveur → client : contenu d'une boutique et Jenny du joueur (ouvre ou rafraîchit l'écran). */
public record OpenShopPayload(String shop, String npc, int jenny, List<Line> sells, List<Line> buys)
        implements CustomPacketPayload {
    /** Une ligne : l'objet, son prix, et (rachat) combien le joueur en a. */
    public record Line(ItemStack stack, int price, int owned) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Line> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Line::stack,
                ByteBufCodecs.VAR_INT, Line::price,
                ByteBufCodecs.VAR_INT, Line::owned,
                Line::new);
    }

    public static final Type<OpenShopPayload> TYPE = new Type<>(HxHMod.id("open_shop"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShopPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenShopPayload::shop,
            ByteBufCodecs.STRING_UTF8, OpenShopPayload::npc,
            ByteBufCodecs.VAR_INT, OpenShopPayload::jenny,
            Line.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenShopPayload::sells,
            Line.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenShopPayload::buys,
            OpenShopPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenShopPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.evandouard.hxh.client.ClientHooks.openShop(payload));
    }
}
