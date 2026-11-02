package com.evandouard.hxh.client;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.hatsu.HatsuKit;
import com.evandouard.hxh.hatsu.Technique;
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

    public static final KeyMapping HATSU_MENU = new KeyMapping("key.hxh.hatsu_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping CAST = new KeyMapping("key.hxh.cast", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping NEXT_TECHNIQUE = new KeyMapping("key.hxh.next_technique", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);

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
        event.register(HATSU_MENU);
        event.register(CAST);
        event.register(NEXT_TECHNIQUE);
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HxHMod.id("aura_bar"), NenClient::renderAuraBar);
        event.registerAbove(VanillaGuiLayers.HOTBAR, HxHMod.id("technique_box"), NenClient::renderTechniqueBox);
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
        while (CAST.consumeClick()) PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.CAST, true));
        while (NEXT_TECHNIQUE.consumeClick()) PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.NEXT_TECHNIQUE, true));
        while (HATSU_MENU.consumeClick()) openHatsuMenu(mc);
        // Soin : touche maintenue, on n'envoie que les changements d'état
        boolean healDown = HEAL.isDown();
        if (healDown != healKeyWasDown) {
            healKeyWasDown = healDown;
            PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.HEAL, healDown));
        }
    }

    /** Ouvre le choix du Hatsu, ou explique pourquoi ce n'est pas (ou plus) possible. */
    private static void openHatsuMenu(Minecraft mc) {
        HunterData data = mc.player.getData(ModAttachments.HUNTER);
        HatsuKit kit = Hatsu.kitOf(data);
        if (kit != null) {
            mc.setScreen(new TechniqueListScreen(kit, null)); // déjà choisi : on consulte sa fiche
        } else if (!data.nenUnlocked()) {
            mc.player.displayClientMessage(Component.translatable("message.hxh.nen_locked"), true);
        } else if (!data.nenRevealed()) {
            mc.player.displayClientMessage(Component.translatable("message.hxh.hatsu_need_divination"), true);
        } else {
            mc.setScreen(new HatsuSelectionScreen());
        }
    }

    /** Encart en bas à droite : technique sélectionnée, coût et état. */
    private static void renderTechniqueBox(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        HunterData data = mc.player.getData(ModAttachments.HUNTER);
        HatsuKit kit = Hatsu.kitOf(data);
        Technique technique = Hatsu.selectedTechnique(data);
        if (kit == null || technique == null) return;

        int w = 130;
        int h = 34;
        int x = graphics.guiWidth() - w - 6;
        int y = graphics.guiHeight() - h - 6;
        // Petite fenêtre : si l'encart chevauche la barre d'objets, on le remonte au-dessus de la faim
        if (x < graphics.guiWidth() / 2 + 95) y = graphics.guiHeight() - h - 52;
        long now = mc.level.getGameTime();
        String refusal = Hatsu.castRefusal(data, technique, now);

        graphics.fill(x, y, x + w, y + h, 0xB0000000);
        graphics.fill(x, y, x + 2, y + h, refusal == null ? 0xFF55FF77 : 0xFFFF5555);

        int index = Math.floorMod(data.selected(), kit.techniques().size()) + 1;
        Component counter = Component.literal(index + "/" + kit.techniques().size());
        int counterWidth = mc.font.width(counter);
        graphics.drawString(mc.font, counter, x + w - 4 - counterWidth, y + 4, 0xFF9A9A9A, true);
        // Nom tronqué avec « … » s'il déborde sur le compteur
        String name = Component.translatable(technique.translationKey()).getString();
        int maxName = w - 6 - counterWidth - 10;
        if (mc.font.width(name) > maxName) name = mc.font.plainSubstrByWidth(name, maxName - mc.font.width("…")) + "…";
        graphics.drawString(mc.font, name, x + 6, y + 4, 0xFFFFD27F, true);

        graphics.drawString(mc.font, Component.translatable("hud.hxh.technique_cost", technique.auraCost()),
                x + 6, y + 14, 0xFFBFE6FF, true);

        Component state;
        if (refusal == null) {
            state = Component.translatable("hud.hxh.technique_ready");
        } else if (refusal.equals("message.hxh.hatsu_cooldown")) {
            float seconds = (data.readyAt(technique.id()) - now) / 20f;
            state = Component.translatable("hud.hxh.technique_cooldown", String.format(java.util.Locale.ROOT, "%.1f", seconds));
        } else {
            state = Component.translatable(refusal);
        }
        graphics.drawString(mc.font, state, x + 6, y + 24, refusal == null ? 0xFF7CFF8A : 0xFFFF7A7A, true);
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
