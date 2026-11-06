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
        this.data = newData;
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
        // Actions (2 lignes de 2) : Hatsu, méditation, Ryu, ouverture des pores de la cible visée
        int aw = (w - 4) / 2;
        int ay = height - 76;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        com.evandouard.hxh.data.HunterData hunter = mc.player.getData(com.evandouard.hxh.data.ModAttachments.HUNTER);
        addRenderableWidget(Button.builder(Component.translatable("screen.hxh.profile.hatsu"), b -> NenClient.openHatsuMenu(mc))
                .bounds(left, ay, aw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.hxh.profile.meditate"), b -> {
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
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
        select(tab);
    }

    private void select(int index) {
        tab = index;
        lastTab = index;
        scroll = 0;
        for (int i = 0; i < tabs.size(); i++) tabs.get(i).active = i != index;
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

    private int visibleRows() {
        return Math.max(1, (height - 60 - 84) / 11);
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
        int top = 56, bottom = height - 82;
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
