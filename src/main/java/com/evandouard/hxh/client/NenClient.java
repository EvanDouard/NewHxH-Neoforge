package com.evandouard.hxh.client;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.network.NenActionPayload;
import com.evandouard.hxh.network.ToggleNenModePayload;
import com.evandouard.hxh.registry.ModBlocks;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Côté client du Nen : touches, jauge d'aura à l'écran, aura visible autour des utilisateurs.
 */
public final class NenClient {
    private static final String CATEGORY = "key.categories.hxh";
    public static final KeyMapping TEN = new KeyMapping("key.hxh.ten", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping ZETSU = new KeyMapping("key.hxh.zetsu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping REN = new KeyMapping("key.hxh.ren", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping HEAL = new KeyMapping("key.hxh.heal", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping MEDITATE = new KeyMapping("key.hxh.meditate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
    public static final KeyMapping OPEN_PORES = new KeyMapping("key.hxh.open_pores", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);

    private static boolean healKeyWasDown = false;

    private static final int WATER_COLOR = 0xFF3F76E4;

    private NenClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(NenClient::onRegisterKeys);
        modBus.addListener(NenClient::onRegisterLayers);
        modBus.addListener(NenClient::onRegisterBlockColors);
        modBus.addListener(EntityRenderersEvent.AddLayers.class, AuraLayer::addToRenderers);
        NeoForge.EVENT_BUS.addListener(NenClient::onClientTick);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TEN);
        event.register(ZETSU);
        event.register(REN);
        event.register(HEAL);
        event.register(MEDITATE);
        event.register(OPEN_PORES);
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
        while (MEDITATE.consumeClick()) PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.MEDITATE, true));
        while (OPEN_PORES.consumeClick()) PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.OPEN_PORES, true));
        // Soin : touche maintenue, on n'envoie que les changements d'état
        boolean healDown = HEAL.isDown();
        if (healDown != healKeyWasDown) {
            healKeyWasDown = healDown;
            PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.HEAL, healDown));
        }
    }

    private static void renderAuraBar(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        HunterData data = mc.player.getData(ModAttachments.HUNTER);
        if (!data.nenUnlocked()) {
            if (data.meditating()) {
                Component text = Component.translatable("hud.hxh.meditating", data.meditationTicks() / 20);
                graphics.drawString(mc.font, text, 8, graphics.guiHeight() - 24, 0xFFE8E0C8, true);
            }
            return;
        }

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

        Component state = data.healing()
                ? Component.translatable("hud.hxh.healing")
                : data.meditating()
                ? Component.translatable("hud.hxh.meditation")
                : Component.translatable(data.mode().translationKey());
        Component label = Component.translatable("hud.hxh.aura", (int) data.aura(), data.auraMax(), state);
        graphics.drawString(mc.font, label, x, y - 10, 0xFFFFFFFF, true);
    }
}
