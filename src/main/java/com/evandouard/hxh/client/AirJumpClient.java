package com.evandouard.hxh.client;

import com.evandouard.hxh.network.AirJumpPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/** Envoie l'intention « saut en l'air » (front montant de la touche de saut hors du sol). Le serveur valide. */
public final class AirJumpClient {
    private static boolean wasDown;

    private AirJumpClient() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.screen != null) { wasDown = false; return; }
        boolean down = mc.options.keyJump.isDown();
        if (down && !wasDown && !p.onGround() && !p.getAbilities().flying && !p.isInWater() && !p.onClimbable()
                && !p.isFallFlying() && !p.isPassenger())
            PacketDistributor.sendToServer(AirJumpPayload.INSTANCE);
        wasDown = down;
    }
}
