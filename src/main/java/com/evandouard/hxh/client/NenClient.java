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

    public static final KeyMapping KO = new KeyMapping("key.hxh.ko", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping GYO = new KeyMapping("key.hxh.gyo", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping EN = new KeyMapping("key.hxh.en", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CATEGORY);
    public static final KeyMapping PROFILE = new KeyMapping("key.hxh.profile", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY);
    private static boolean castWasDown;
    public static final KeyMapping CAST = new KeyMapping("key.hxh.cast", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping NEXT_TECHNIQUE = new KeyMapping("key.hxh.next_technique", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);

    private static boolean healKeyWasDown = false;
    private static boolean strikeWasDown = false;
    /** Tick de jeu où la touche Frappe a été enfoncée (pour dessiner la barre), -1 si relâchée. */
    private static long strikeStart = -1;

    private static final int WATER_COLOR = 0xFF3F76E4;

    private NenClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(NenClient::onRegisterKeys);
        modBus.addListener(NenClient::onRegisterLayers);
        MangaHud.register(modBus);
        com.evandouard.hxh.client.look.CharacterLooks.register(modBus);
        modBus.addListener(NenClient::onRegisterBlockColors);
        modBus.addListener(EntityRenderersEvent.AddLayers.class, AuraLayer::addToRenderers);
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
                e -> {
                    e.registerEntityRenderer(com.evandouard.hxh.registry.ModEntities.NPC.get(), NpcRenderer::new);
                    e.registerEntityRenderer(com.evandouard.hxh.registry.ModEntities.ARENA_FIGHTER.get(), ArenaFighterRenderer::new);
                    e.registerEntityRenderer(com.evandouard.hxh.registry.ModEntities.EXAM_CANDIDATE.get(), ExamCandidateRenderer::new);
                    e.registerEntityRenderer(com.evandouard.hxh.registry.ModEntities.NEN_PROJECTILE.get(), NenProjectileRenderer::new);
                    e.registerEntityRenderer(com.evandouard.hxh.registry.ModEntities.FOXBEAR.get(), FoxbearRenderer::new);
                    e.registerEntityRenderer(com.evandouard.hxh.registry.ModEntities.SPIDER_EAGLE.get(), SpiderEagleRenderer::new);
                    e.registerEntityRenderer(com.evandouard.hxh.registry.ModEntities.MAN_FACED_APE.get(), ManFacedApeRenderer::new);
                    e.registerEntityRenderer(com.evandouard.hxh.registry.ModEntities.CHIMERA_ANT.get(), ChimeraAntRenderer::new);
                    e.registerEntityRenderer(com.evandouard.hxh.registry.ModEntities.KIRIKO.get(), KirikoRenderer::new);
                    e.registerEntityRenderer(com.evandouard.hxh.registry.ModEntities.GREAT_STAMP.get(), GreatStampRenderer::new);
                    e.registerBlockEntityRenderer(com.evandouard.hxh.registry.ModBlockEntities.MARKER.get(),
                            com.evandouard.hxh.marker.MarkerLabelRenderer::new);
                });
        NeoForge.EVENT_BUS.addListener(NenClient::onClientTick);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TEN);
        event.register(ZETSU);
        event.register(REN);
        event.register(HEAL);
        event.register(KO);
        event.register(GYO);
        event.register(EN);
        event.register(PROFILE);
        event.register(CAST);
        event.register(NEXT_TECHNIQUE);
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HxHMod.id("technique_box"), NenClient::renderTechniqueBox);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, HxHMod.id("strike_bar"), NenClient::renderStrikeBar);
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
        while (KO.consumeClick()) PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.KO, true));
        while (GYO.consumeClick()) PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.GYO, true));
        while (EN.consumeClick()) PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.EN, true));
        // Touche de technique : appui (clic ou passage à « enfoncée »), puis relâchement — qui lance les techniques
        // qu'on charge (Pierre, frappe chargée). Un simple clic sans maintien est relâché aussitôt.
        boolean clicked = false;
        while (CAST.consumeClick()) clicked = true;
        boolean castDown = CAST.isDown();
        if (!castWasDown && (castDown || clicked)) PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.CAST, true));
        if ((castWasDown && !castDown) || (clicked && !castDown && !castWasDown)) {
            PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.CAST, false));
        }
        castWasDown = castDown;
        while (NEXT_TECHNIQUE.consumeClick()) PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.NEXT_TECHNIQUE, true));
        while (PROFILE.consumeClick()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(com.evandouard.hxh.network.RequestProfilePayload.INSTANCE);
        }
        SensesClient.tick(mc);
        // Barre de la frappe chargée : touche de technique maintenue sur « Frappe chargée » prête
        HunterData me = mc.player.getData(ModAttachments.HUNTER);
        boolean charging = castDown && Hatsu.selectedTechnique(me) == com.evandouard.hxh.hatsu.Techniques.BASIC_CHARGED;
        if (charging != strikeWasDown) {
            strikeWasDown = charging;
            boolean ready = Hatsu.castRefusal(me, com.evandouard.hxh.hatsu.Techniques.BASIC_CHARGED, mc.level.getGameTime()) == null;
            strikeStart = charging && ready ? mc.level.getGameTime() : -1;
        }
        // Soin : touche maintenue, on n'envoie que les changements d'état
        boolean healDown = HEAL.isDown();
        if (healDown != healKeyWasDown) {
            healKeyWasDown = healDown;
            PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.HEAL, healDown));
        }
    }

    /** Ouvre le choix du Hatsu, ou explique pourquoi ce n'est pas (ou plus) possible. */
    /** Ouvrir les pores de la cible visée (bouton du carnet de Hunter). */
    public static void openPores() {
        PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.OPEN_PORES, true));
    }

    /** Méditer (bouton du carnet de Hunter). */
    public static void meditate() {
        PacketDistributor.sendToServer(new NenActionPayload(NenActionPayload.Action.MEDITATE, true));
    }

    /** Écran de répartition du Ryu (bouton du carnet de Hunter). */
    public static void openRyu(Minecraft mc) {
        mc.setScreen(new RyuScreen());
    }

    /** Choix ou fiche du Hatsu (bouton du carnet de Hunter). */
    public static void openHatsuMenu(Minecraft mc) {
        HunterData data = mc.player.getData(ModAttachments.HUNTER);
        HatsuKit kit = Hatsu.kitOf(data);
        if (mc.player.isCreative()) {
            mc.setScreen(new HatsuSelectionScreen()); // créatif : on peut changer de personnage librement
        } else if (kit != null) {
            mc.setScreen(new TechniqueListScreen(kit, null)); // déjà choisi : on consulte sa fiche
        } else if (!data.nenUnlocked()) {
            mc.player.displayClientMessage(Component.translatable("message.hxh.nen_locked"), true);
        } else if (!data.nenRevealed()) {
            mc.player.displayClientMessage(Component.translatable("message.hxh.hatsu_need_divination"), true);
        } else {
            mc.setScreen(new HatsuSelectionScreen());
        }
    }

    /** Barre de la frappe chargée, sous le viseur : rouge = ratée, vert = parfaite. */
    private static void renderStrikeBar(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (strikeStart < 0 || mc.level == null || mc.options.hideGui) return;
        long held = mc.level.getGameTime() - strikeStart;
        float value = com.evandouard.hxh.combat.Combat.chargeValue(held);
        int w = 60;
        int h = 6;
        int x = graphics.guiWidth() / 2 - w / 2;
        int y = graphics.guiHeight() / 2 + 12;
        int fail = (int) (w * com.evandouard.hxh.combat.Combat.CHARGE_FAIL_BELOW);
        int perfect = (int) (w * com.evandouard.hxh.combat.Combat.CHARGE_PERFECT_FROM);
        graphics.fill(x + 1, y + 1, x + w + 3, y + h + 3, MangaHud.INK);
        graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, MangaHud.INK);
        graphics.fill(x, y, x + w, y + h, MangaHud.PAPER);
        graphics.fill(x, y, x + fail, y + h, 0xFFFFC9C0);
        graphics.fill(x + perfect, y, x + w, y + h, 0xFFC8F5D2);
        int fill = (int) (w * value);
        int color = value < com.evandouard.hxh.combat.Combat.CHARGE_FAIL_BELOW ? 0xFFFF5A3C
                : value >= com.evandouard.hxh.combat.Combat.CHARGE_PERFECT_FROM ? 0xFF52D273 : MangaHud.GOLD;
        graphics.fill(x, y + 1, x + fill, y + h - 1, color);
        graphics.fill(x + perfect, y - 2, x + perfect + 1, y + h + 2, MangaHud.INK);
    }

    /** Encart en bas à droite : technique sélectionnée, coût et état. */
    private static void renderTechniqueBox(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        HunterData data = mc.player.getData(ModAttachments.HUNTER);
        Technique technique = Hatsu.selectedTechnique(data);
        if (technique == null) return;
        java.util.List<String> ids = Hatsu.techniqueIds(data);

        int w = 130;
        int h = 34;
        int x = graphics.guiWidth() - w - 6;
        int y = graphics.guiHeight() - h - 6;
        // Petite fenêtre : si l'encart chevauche la barre d'objets, on le remonte au-dessus de la faim
        if (x < graphics.guiWidth() / 2 + 95) y = graphics.guiHeight() - h - 52;
        long now = mc.level.getGameTime();
        String refusal = Hatsu.castRefusal(data, technique, now);

        MangaHud.panel(graphics, x, y, w, h, MangaHud.PAPER, 3);
        graphics.fill(x + 2, y + 2, x + 6, y + h - 2, refusal == null ? 0xFF52D273 : 0xFFFF5A3C);

        int index = Math.floorMod(data.selected(), ids.size()) + 1;
        Component counter = Component.literal(index + "/" + ids.size());
        int counterWidth = mc.font.width(counter);
        graphics.drawString(mc.font, counter, x + w - 5 - counterWidth, y + 5, 0xFF777777, false);
        // Nom tronqué avec « … » s'il déborde sur le compteur
        String name = Component.translatable(technique.translationKey()).getString();
        int maxName = w - 10 - counterWidth - 10;
        // Le gras ajoute un pixel par lettre : on mesure le texte tel qu'il est dessiné
        java.util.function.ToIntFunction<String> bold = t -> mc.font.width(Component.literal(t).withStyle(net.minecraft.ChatFormatting.BOLD));
        if (bold.applyAsInt(name) > maxName) {
            while (name.length() > 1 && bold.applyAsInt(name + "…") > maxName) name = name.substring(0, name.length() - 1);
            name = name.stripTrailing() + "…";
        }
        graphics.drawString(mc.font, Component.literal(name).withStyle(net.minecraft.ChatFormatting.BOLD), x + 10, y + 5, MangaHud.INK, false);

        // Ging : la copie affiche ses utilisations restantes
        com.evandouard.hxh.hatsu.Technique copied = com.evandouard.hxh.hatsu.Characters.gingCopied(data);
        Component costLine = Component.translatable("hud.hxh.technique_cost", technique.auraCost());
        if (copied == technique) costLine = costLine.copy().append(" · ").append(Component.translatable("hud.hxh.copy_uses",
                Math.round(data.kitValue(com.evandouard.hxh.hatsu.Characters.GING_COPY_USES))));
        graphics.drawString(mc.font, costLine, x + 10, y + 14, 0xFF1F6FA8, false);

        Component state;
        if (refusal == null) {
            state = Component.translatable("hud.hxh.technique_ready");
        } else if (refusal.equals("message.hxh.hatsu_nen_level")) {
            state = Component.translatable("hud.hxh.technique_nen_level", technique.minNenLevel());
        } else if (refusal.equals("message.hxh.hatsu_cooldown")) {
            float seconds = (data.readyAt(technique.id()) - now) / 20f;
            state = Component.translatable("hud.hxh.technique_cooldown", String.format(java.util.Locale.ROOT, "%.1f", seconds));
        } else {
            state = Component.translatable(refusal);
        }
        graphics.drawString(mc.font, state, x + 10, y + 23, refusal == null ? 0xFF1E8A3A : 0xFFB3261E, false);
    }

}
