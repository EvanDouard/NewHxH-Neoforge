package com.evandouard.hxh.client;

import com.evandouard.hxh.network.ProfilePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Carnet de Hunter (touche O) : statistiques et Nen, quêtes, Tour Céleste et Jenny, par onglets ;
 * boutons pour le Hatsu, la méditation, le Ryu et l'ouverture des pores de la cible visée.
 * La molette fait défiler les longues pages.
 */
public class ProfileScreen extends Screen {
    private static int lastTab = 0;
    private ProfilePayload data;
    private int tab;
    private int scroll;
    private final List<Button> tabs = new ArrayList<>();

    public ProfileScreen(ProfilePayload data) {
        super(Component.translatable("screen.hxh.profile.title"));
        this.data = data;
        this.tab = lastTab;
    }

    /** Nouvelles données reçues pendant que l'écran est ouvert. */
    public void update(ProfilePayload newData) {
        if (newData.equals(this.data)) return; // rien n'a changé : on ne reconstruit pas l'écran
        this.data = newData;
        rebuildWidgets();
    }

    private int refreshTicks;

    /** Stats et XP en temps réel : on redemande le carnet au serveur chaque seconde tant qu'il est ouvert. */
    @Override
    public void tick() {
        super.tick();
        if (++refreshTicks % 20 == 0) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(com.evandouard.hxh.network.RequestProfilePayload.INSTANCE);
        }
    }

    private int panelW() {
        return Math.min(340, width - 20);
    }

    @Override
    protected void init() {
        tabs.clear();
        String[] keys = {"screen.hxh.profile.tab_character", "screen.hxh.profile.tab_quests", "screen.hxh.profile.tab_arena"};
        int w = panelW();
        int left = width / 2 - w / 2;
        int bw = (w - 8) / 3;
        for (int i = 0; i < keys.length; i++) {
            int index = i;
            tabs.add(addRenderableWidget(Button.builder(Component.translatable(keys[i]), b -> select(index))
                    .bounds(left + i * (bw + 4), 28, bw, 20).build()));
        }
        if (tab == 1) addTrackButtons(left, w);
        else {
            // Actions (2 lignes de 2) : Hatsu, méditation, Ryu, ouverture des pores de la cible visée
            int aw = (w - 4) / 2;
            int ay = height - 76;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            com.evandouard.hxh.data.HunterData hunter = mc.player.getData(com.evandouard.hxh.data.ModAttachments.HUNTER);
            addRenderableWidget(Button.builder(Component.translatable("screen.hxh.profile.hatsu"), b -> NenClient.openHatsuMenu(mc))
                    .bounds(left, ay, aw, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("netero".equals(hunter.kit()) ? "screen.hxh.profile.pray" : "screen.hxh.profile.meditate"), b -> {
                onClose();
                NenClient.meditate();
            }).bounds(left + aw + 4, ay, aw, 20).build());
            Button ryu = addRenderableWidget(Button.builder(Component.translatable("screen.hxh.profile.ryu"), b -> NenClient.openRyu(mc))
                    .bounds(left, ay + 24, aw, 20).build());
            ryu.active = hunter.nenUnlocked();
            Button pores = addRenderableWidget(Button.builder(Component.translatable("screen.hxh.profile.open_pores"), b -> {
                onClose();
                NenClient.openPores();
            }).bounds(left + aw + 4, ay + 24, aw, 20).build());
            pores.active = hunter.nenUnlocked();
            if (tab == 0) {
                // Vitesse max : pour aller très vite seulement quand on le veut
                addRenderableWidget(new SpeedSlider(left, ay - 24, w - 88, 20, hunter.speedCap()));
                // Forme de l'En (maîtrise 50)
                Button en = addRenderableWidget(Button.builder(Component.translatable("screen.hxh.profile.en_shape"),
                        b -> mc.setScreen(new EnShapeScreen(this))).bounds(left + w - 84, ay - 24, 84, 20).build());
                en.active = hunter.masteryLevel(com.evandouard.hxh.data.Mastery.EN) >= com.evandouard.hxh.nen.EnShape.UNLOCK_LEVEL;
                if (!en.active) en.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                        "screen.hxh.en_shape.locked", com.evandouard.hxh.nen.EnShape.UNLOCK_LEVEL)));
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
        select(tab);
    }

    /** Curseur « Vitesse max » : part du bonus de Vitesse utilisée, envoyée au serveur au relâchement. */
    private static final class SpeedSlider extends net.minecraft.client.gui.components.AbstractSliderButton {
        SpeedSlider(int x, int y, int w, int h, float value) {
            super(x, y, w, h, Component.empty(), value);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.hxh.profile.speed_cap", Math.round(value * 100)));
        }

        @Override
        protected void applyValue() {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.evandouard.hxh.network.SpeedCapPayload((float) value));
        }
    }

    private void select(int index) {
        boolean changed = tab != index;
        tab = index;
        lastTab = index;
        scroll = 0;
        for (int i = 0; i < tabs.size(); i++) tabs.get(i).active = i != index;
        if (changed) rebuildWidgets();
    }

    /** Onglet Quêtes : un bouton « Suivre » par autre quête en cours (jusqu'à 4). */
    private void addTrackButtons(int left, int w) {
        int aw = (w - 4) / 2;
        int ay = height - 76;
        int i = 0;
        for (String id : data.runningQuests()) {
            if (id.equals(data.trackedQuest()) || i >= 4) continue;
            String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            Component title = Component.translatable("quest.hxh." + path);
            addRenderableWidget(Button.builder(Component.translatable("screen.hxh.profile.track", title),
                            b -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.evandouard.hxh.network.TrackQuestPayload(id)))
                    .bounds(left + (i % 2) * (aw + 4), ay + (i / 2) * 24, aw, 20).build());
            i++;
        }
    }

    private List<Component> lines() {
        return switch (tab) {
            case 1 -> data.quests();
            case 2 -> data.arena();
            default -> data.character();
        };
    }

    private List<FormattedCharSequence> wrapped() {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (Component line : lines()) {
            if (line.getString().isEmpty()) out.add(FormattedCharSequence.EMPTY);
            else out.addAll(font.split(line, panelW() - 16));
        }
        return out;
    }

    /** Bas du panneau de texte : au-dessus du curseur « Vitesse max » sur l'onglet Personnage. */
    private int panelBottom() {
        return tab == 0 ? height - 106 : height - 82;
    }

    private int visibleRows() {
        return Math.max(1, (panelBottom() - 56) / 11);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, wrapped().size() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY) * 2));
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int w = panelW();
        int left = width / 2 - w / 2;
        g.drawCenteredString(font, title, width / 2, 12, 0xFFD27F);
        int top = 56, bottom = panelBottom();
        g.fill(left, top - 4, left + w, bottom, 0xAA000000);
        List<FormattedCharSequence> rows = wrapped();
        int y = top;
        for (int i = scroll; i < rows.size() && y + 9 <= bottom; i++) {
            g.drawString(font, rows.get(i), left + 8, y, 0xFFFFFF);
            y += 11;
        }
        if (rows.size() > visibleRows()) {
            g.drawString(font, Component.literal(scroll + visibleRows() < rows.size() ? "▼" : "▲"), left + w - 12, bottom - 12, 0xAAAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
