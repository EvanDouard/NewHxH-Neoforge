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
        String[] keys = {"screen.hxh.profile.tab_character", "screen.hxh.profile.tab_quests", "screen.hxh.profile.tab_arena", "screen.hxh.profile.tab_nen"};
        int w = panelW();
        int left = width / 2 - w / 2;
        int bw = (w - 4 * (keys.length - 1)) / keys.length;
        for (int i = 0; i < keys.length; i++) {
            int index = i;
            tabs.add(addRenderableWidget(Button.builder(Component.translatable(keys[i]), b -> select(index))
                    .bounds(left + i * (bw + 4), 28, bw, 20).build()));
        }
        if (tab == 1) addTrackButtons(left, w);
        else if (tab != 3) {
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
        return tab == 0 ? height - 106 : tab == 3 ? height - 34 : height - 82;
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
        if (tab == 3) {
            renderGraph(g, left, top, w, bottom, mouseX, mouseY);
            return;
        }
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

    // ---------- Onglet « Graphe du Nen » : dépendances entre techniques et avancement ----------

    private record GNode(com.evandouard.hxh.nen.NenLearning.Ability ability, int layer, boolean learned,
                         List<String[]> reqs, int x, int y) {
        float progress() {
            if (learned) return 1f;
            if (reqs.isEmpty()) return 0f;
            float sum = 0;
            for (String[] r : reqs) sum += Math.min(1f, Integer.parseInt(r[2]) / (float) Math.max(1, Integer.parseInt(r[1])));
            return sum / reqs.size();
        }
    }

    private static final int NODE_W = 58, NODE_H = 20;
    private String selected = "";
    private final java.util.Map<String, GNode> graphNodes = new java.util.LinkedHashMap<>();

    /** Place les nœuds : une rangée par couche (base, avancées, combinaison, maîtrise), répartis sur la largeur. */
    private void layoutGraph(int left, int top, int w, int bottom) {
        graphNodes.clear();
        int[] perLayer = new int[4];
        List<String[]> parsed = new ArrayList<>();
        for (String line : data.graph()) {
            String[] p = line.split("\\|", -1);
            parsed.add(p);
            perLayer[Integer.parseInt(p[1])]++;
        }
        int detail = 46;
        int usable = bottom - top - detail - NODE_H;
        int[] seen = new int[4];
        for (String[] p : parsed) {
            var ability = com.evandouard.hxh.nen.NenLearning.Ability.byId(p[0]);
            if (ability == null) continue;
            int layer = Integer.parseInt(p[1]);
            List<String[]> reqs = new ArrayList<>();
            if (!p[3].isEmpty()) for (String r : p[3].split(",")) reqs.add(r.split(":"));
            int n = perLayer[layer];
            int slot = w / n;
            int x = left + slot * seen[layer]++ + slot / 2 - NODE_W / 2;
            int y = top + 2 + usable * layer / 3;
            graphNodes.put(p[0], new GNode(ability, layer, "1".equals(p[2]), reqs, x, y));
        }
    }

    private GNode nodeAt(double mx, double my) {
        for (GNode n : graphNodes.values())
            if (mx >= n.x() && mx < n.x() + NODE_W && my >= n.y() && my < n.y() + NODE_H) return n;
        return null;
    }

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color, boolean dashed) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            if (dashed && (i / 3) % 2 == 1) continue;
            int x = x0 + (x1 - x0) * i / Math.max(1, steps), y = y0 + (y1 - y0) * i / Math.max(1, steps);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void renderGraph(GuiGraphics g, int left, int top, int w, int bottom, int mouseX, int mouseY) {
        layoutGraph(left, top, w, bottom);
        GNode hover = nodeAt(mouseX, mouseY);
        String focus = hover != null ? hover.ability().id() : selected;
        // Liens : plus clairs quand la technique parente est acquise, en surbrillance autour de la technique visée
        for (var e : com.evandouard.hxh.nen.NenTree.edges()) {
            GNode a = graphNodes.get(e.from().id()), b = graphNodes.get(e.to().id());
            if (a == null || b == null) continue;
            boolean lit = focus.equals(e.from().id()) || focus.equals(e.to().id());
            int color = lit ? 0xFFFFD27F : a.learned() ? 0xFF5FBF6A : 0xFF666666;
            line(g, a.x() + NODE_W / 2, a.y() + NODE_H, b.x() + NODE_W / 2, b.y(), color, e.uncertain());
        }
        for (GNode n : graphNodes.values()) {
            boolean active = n.ability().id().equals(focus);
            int border = n.learned() ? 0xFF5FBF6A : n.progress() > 0 ? 0xFFE0A030 : 0xFF777777;
            int fill = n.learned() ? 0xFF1F4D26 : 0xFF222222;
            g.fill(n.x() - 1, n.y() - 1, n.x() + NODE_W + 1, n.y() + NODE_H + 1, active ? 0xFFFFFFFF : border);
            g.fill(n.x(), n.y(), n.x() + NODE_W, n.y() + NODE_H, fill);
            if (!n.learned() && n.progress() > 0)
                g.fill(n.x(), n.y() + NODE_H - 3, n.x() + Math.round(NODE_W * n.progress()), n.y() + NODE_H, 0xFFB8801C);
            Component name = Component.translatable(n.ability().translationKey());
            g.drawCenteredString(font, name, n.x() + NODE_W / 2, n.y() + 4, n.learned() ? 0xFFFFFF : 0xBBBBBB);
        }
        // Détail de la technique visée ou choisie
        int dy = bottom - 44;
        GNode info = graphNodes.get(focus);
        if (info == null) {
            g.drawString(font, Component.translatable("screen.hxh.nen_graph.hint"), left + 8, dy + 4, 0xAAAAAA);
            return;
        }
        Component status = Component.translatable(info.learned() ? "screen.hxh.nen_graph.unlocked"
                : info.progress() > 0 ? "screen.hxh.nen_graph.progress" : "screen.hxh.nen_graph.locked", Math.round(info.progress() * 100));
        g.drawString(font, Component.translatable(info.ability().translationKey()).append(" – ").append(status), left + 8, dy, 0xFFD27F);
        StringBuilder sb = new StringBuilder();
        if (info.reqs().isEmpty()) sb.append(Component.translatable("screen.hxh.nen_graph.base").getString());
        for (String[] r : info.reqs()) {
            if (sb.length() > 0) sb.append("   ");
            boolean done = Integer.parseInt(r[2]) >= Integer.parseInt(r[1]);
            if (r[0].equals("hatsu")) sb.append(done ? "§a" : "§7").append(Component.translatable("screen.hxh.nen_graph.hatsu").getString());
            else sb.append(done ? "§a" : "§7").append(Component.translatable("mastery.hxh." + r[0]).getString())
                    .append(' ').append(Math.min(Integer.parseInt(r[2]), 999)).append('/').append(r[1]);
            sb.append("§r");
        }
        int ry = dy + 12;
        for (FormattedCharSequence l : font.split(Component.literal(sb.toString()), w - 16)) {
            g.drawString(font, l, left + 8, ry, 0xFFFFFF);
            ry += 10;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (tab == 3) {
            GNode n = nodeAt(mouseX, mouseY);
            selected = n == null ? "" : n.ability().id();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
