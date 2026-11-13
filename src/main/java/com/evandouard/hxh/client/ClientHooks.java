package com.evandouard.hxh.client;

import net.minecraft.client.Minecraft;

/** Points d'entrée client appelés par les paquets (gardés à part pour ne pas charger de classes client sur le serveur). */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openProfile(com.evandouard.hxh.network.ProfilePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ProfileScreen open) open.update(payload);
        else mc.setScreen(new ProfileScreen(payload));
    }

    public static void openElevator(com.evandouard.hxh.network.OpenElevatorPayload payload) {
        Minecraft.getInstance().setScreen(new ElevatorScreen(payload.brackets(), payload.current(), payload.room()));
    }

    public static void openShop(com.evandouard.hxh.network.OpenShopPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        // Écran déjà ouvert sur cette boutique : on le met à jour sans le refermer
        if (mc.screen instanceof ShopScreen open && open.shopId().equals(payload.shop())) open.update(payload);
        else mc.setScreen(new ShopScreen(payload));
    }

    public static void openAuction(com.evandouard.hxh.network.OpenAuctionPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AuctionScreen open && open.auctionId().equals(payload.auction())) open.update(payload);
        else mc.setScreen(new AuctionScreen(payload));
    }

    /** Bras de fer : ouvre l'écran au premier état, puis le met à jour. */
    public static void armWrestle(com.evandouard.hxh.network.ArmWrestleStatePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ArmWrestleScreen open) open.update(payload);
        else if (payload.result() == 0) mc.setScreen(new ArmWrestleScreen(payload));
    }

    public static void openComputer(com.evandouard.hxh.network.OpenComputerPayload payload) {
        Minecraft.getInstance().setScreen(new HunterComputerScreen(payload));
    }
}
