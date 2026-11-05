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
}
