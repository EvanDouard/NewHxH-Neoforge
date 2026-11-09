package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.shop.Shop;
import com.evandouard.hxh.shop.ShopRegistry;
import com.evandouard.hxh.shop.Shops;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → serveur : intention d'acheter l'offre {@code index} ou de vendre la ligne {@code index}.
 * Le serveur vérifie qu'un PNJ de cette boutique est tout près, les Jenny et l'inventaire.
 */
public record ShopActionPayload(String shop, int index, boolean sell, boolean all) implements CustomPacketPayload {
    public static final double REACH = 8;
    public static final Type<ShopActionPayload> TYPE = new Type<>(HxHMod.id("shop_action"));
    public static final StreamCodec<ByteBuf, ShopActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ShopActionPayload::shop,
            ByteBufCodecs.VAR_INT, ShopActionPayload::index,
            ByteBufCodecs.BOOL, ShopActionPayload::sell,
            ByteBufCodecs.BOOL, ShopActionPayload::all,
            ShopActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShopActionPayload p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ResourceLocation id = ResourceLocation.tryParse(p.shop());
            Shop shop = id == null ? null : ShopRegistry.get(id);
            if (shop == null) return;
            boolean near = !player.level().getEntities(com.evandouard.hxh.registry.ModEntities.NPC.get(),
                    player.getBoundingBox().inflate(REACH), n -> n.npcId().equals(shop.npc())).isEmpty();
            if (!near) return;
            if (p.sell()) Shops.sell(player, shop, p.index(), p.all());
            else Shops.buy(player, shop, p.index());
            Shops.open(player, shop);
        });
    }
}
