package com.evandouard.hxh.client;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.network.ToggleNenModePayload;
import com.evandouard.hxh.registry.ModBlocks;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * Côté client du Nen : touches, jauge d'aura à l'écran, aura visible autour des utilisateurs.
 */
public final class NenClient {
    private static final String CATEGORY = "key.categories.hxh";
    public static final KeyMapping TEN = new KeyMapping("key.hxh.ten", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping ZETSU = new KeyMapping("key.hxh.zetsu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping REN = new KeyMapping("key.hxh.ren", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    private static final int WATER_COLOR = 0xFF3F76E4;

    private NenClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(NenClient::onRegisterKeys);
        modBus.addListener(NenClient::onRegisterLayers);
        modBus.addListener(NenClient::onRegisterBlockColors);
        NeoForge.EVENT_BUS.addListener(NenClient::onClientTick);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TEN);
        event.register(ZETSU);
        event.register(REN);
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HxHMod.id("aura_bar"), NenClient::renderAuraBar);
    }

    private static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? WATER_COLOR : -1,
                ModBlocks.WATER_DIVINATION.get());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        while (TEN.consumeClick()) PacketDistributor.sendToServer(new ToggleNenModePayload(NenMode.TEN.id()));
        while (ZETSU.consumeClick()) PacketDistributor.sendToServer(new ToggleNenModePayload(NenMode.ZETSU.id()));
        while (REN.consumeClick()) PacketDistributor.sendToServer(new ToggleNenModePayload(NenMode.REN.id()));
        spawnAuraParticles(mc);
    }

    /** L'aura (Ten, Ren) n'est visible que si on maîtrise soi-même le Nen. */
    private static void spawnAuraParticles(Minecraft mc) {
        if (mc.isPaused() || !mc.player.getData(ModAttachments.HUNTER).nenUnlocked()) return;
        for (Player p : mc.level.players()) {
            if (!p.hasData(ModAttachments.HUNTER)) continue;
            NenMode mode = p.getData(ModAttachments.HUNTER).mode();
            if (mode != NenMode.TEN && mode != NenMode.REN) continue;
            boolean ren = mode == NenMode.REN;
            int count = ren ? 4 : 1;
            DustParticleOptions dust = ren
                    ? new DustParticleOptions(new Vector3f(1f, 0.25f, 0.2f), 1.1f)
                    : new DustParticleOptions(new Vector3f(0.75f, 0.9f, 1f), 0.8f);
            for (int i = 0; i < count; i++) {
                double x = p.getX() + (p.getRandom().nextDouble() - 0.5) * p.getBbWidth() * 1.6;
                double y = p.getY() + p.getRandom().nextDouble() * p.getBbHeight();
                double z = p.getZ() + (p.getRandom().nextDouble() - 0.5) * p.getBbWidth() * 1.6;
                mc.level.addParticle(dust, x, y, z, 0, 0.02, 0);
            }
        }
    }

    private static void renderAuraBar(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        HunterData data = mc.player.getData(ModAttachments.HUNTER);
        if (!data.nenUnlocked()) return;

        int width = 80;
        int x = 8;
        int y = graphics.guiHeight() - 14;
        float ratio = data.auraMax() > 0 ? data.aura() / data.auraMax() : 0;
        int color = switch (data.mode()) {
            case TEN -> 0xFFBFE6FF;
            case ZETSU -> 0xFF6E6E6E;
            case REN -> 0xFFFF4A3A;
            case NONE -> 0xFFE8E0C8;
        };
        graphics.fill(x - 1, y - 1, x + width + 1, y + 6, 0xC0000000);
        graphics.fill(x, y, x + (int) (width * ratio), y + 5, color);

        Component label = Component.translatable("hud.hxh.aura", (int) data.aura(), data.auraMax(),
                Component.translatable(data.mode().translationKey()));
        graphics.drawString(mc.font, label, x, y - 10, 0xFFFFFFFF, true);
    }
}
