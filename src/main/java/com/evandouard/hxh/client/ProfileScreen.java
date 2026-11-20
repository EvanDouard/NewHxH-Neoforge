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
 * Carnet de Hunter (touche O), par onglets : Personnage (stats cliquables, fiches Nen et famille, boutons de
 * réglage : Hatsu, méditation, Ryu, pores, vitesse, En), Quêtes, Tour Céleste, Nen (graphe des techniques) et Aura
 * (aura, soin par le Nen, régénération). Cliquer une stat ou une technique ouvre sa fiche : description, XP, niveau
 * et barre de paliers (survol = ce que fait la technique à ce niveau, efficacité en aura comprise).
 * La molette fait défiler les longues pages ; Échap ferme d'abord la fiche ouverte.
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
        return Math.min(380, width - 20);
    }

    @Override
    protected void init() {
        tabs.clear();
        String[] keys = {"screen.hxh.profile.tab_character", "screen.hxh.profile.tab_quests", "screen.hxh.profile.tab_arena",
                "screen.hxh.profile.tab_nen", "screen.hxh.profile.tab_aura"};
        int w = panelW();
        int left = width / 2 - w / 2;
        int bw = (w - 4 * (keys.length - 1)) / keys.length;
        for (int i = 0; i < keys.length; i++) {
            int index = i;
            tabs.add(addRenderableWidget(Button.builder(Component.translatable(keys[i]), b -> select(index))
                    .bounds(left + i * (bw + 4), 28, bw, 20).build()));
        }
        if (!selected.isEmpty() && (tab == 0 || tab == 3 || tab == 4)) {
            addRenderableWidget(Button.builder(Component.translatable("screen.hxh.detail.back"), b -> closeDetail())
                    .bounds(left + w - 64, 53, 60, 14).build());
            // Hatsu et Ryu : leur bouton de réglage est dans leur fiche (catégorie Nen)
            net.minecraft.client.Minecraft mcd = net.minecraft.client.Minecraft.getInstance();
            com.evandouard.hxh.data.HunterData hd = mcd.player.getData(com.evandouard.hxh.data.ModAttachments.HUNTER);
            if (selected.equals("hatsu")) {
                addRenderableWidget(Button.builder(Component.translatable("screen.hxh.profile.hatsu"), b -> NenClient.openHatsuMenu(mcd))
                        .bounds(left + w - 64 - 134, 53, 130, 14).build());
            } else if (selected.equals("ryu")) {
                Button ryuB = addRenderableWidget(Button.builder(Component.translatable("screen.hxh.profile.ryu"), b -> NenClient.openRyu(mcd))
                        .bounds(left + w - 64 - 134, 53, 130, 14).build());
                ryuB.active = hd.nenUnlocked();
            }
        } else if (tab == 1) addTrackButtons(left, w);
        else if (tab == 0 || tab == 2) {
            // Actions : méditation et ouverture des pores de la cible visée (Hatsu et Ryu sont dans leur fiche, onglet Nen)
            int aw = (w - 4) / 2;
            int ay = height - 76;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            com.evandouard.hxh.data.HunterData hunter = mc.player.getData(com.evandouard.hxh.data.ModAttachments.HUNTER);
            addRenderableWidget(Button.builder(Component.translatable("netero".equals(hunter.kit()) ? "screen.hxh.profile.pray" : "screen.hxh.profile.meditate"), b -> {
                onClose();
                NenClient.meditate();
            }).bounds(left, ay + 24, aw, 20).build());
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
        if (changed) selected = "";
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
            case 0 -> switch (selected) {
                case "txt:identity" -> data.extra().identity();
                case "txt:family" -> data.extra().family();
                default -> data.character();
            };
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
        if (tab == 0) return selected.isEmpty() ? height - 106 : height - 34;
        return tab >= 3 ? height - 34 : height - 82;
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
        parseDetails();
        if (!selected.isEmpty() && (tab == 3 || tab == 4 || tab == 0)) {
            if (tab == 3) layoutGraph(left, top, w, bottom);
            renderDetail(g, left, top, w, bottom, mouseX, mouseY);
            return;
        }
        if (tab == 3) {
            renderGraph(g, left, top, w, bottom, mouseX, mouseY);
            return;
        }
        if (tab == 0 || tab == 4) {
            renderCards(g, left, top, w, mouseX, mouseY);
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
    /** Niveau de Nen, plafond des maîtrises, % vers le niveau suivant. */
    private int[] nenMeta = {0, 5, 0};
    private final java.util.Map<String, GNode> graphNodes = new java.util.LinkedHashMap<>();

    /** Place les nœuds : une rangée par couche (base, avancées, combinaison, maîtrise), répartis sur la largeur. */
    private void layoutGraph(int left, int top, int w, int bottom) {
        graphNodes.clear();
        int[] perLayer = new int[4];
        List<String[]> parsed = new ArrayList<>();
        for (String line : data.graph()) {
            String[] p = line.split("\\|", -1);
            if (p[0].equals("meta")) {
                nenMeta = new int[]{Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])};
                continue;
            }
            parsed.add(p);
            perLayer[Integer.parseInt(p[1])]++;
        }
        int detail = 46;
        top += 14; // bandeau du niveau de Nen
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
        // Bandeau : niveau de Nen, plafond des maîtrises et barre d'XP vers le niveau suivant
        g.drawString(font, Component.translatable("screen.hxh.nen_graph.header", nenMeta[0], nenMeta[1], nenMeta[2]), left + 8, top - 1, 0x9FD3FF);
        g.fill(left + 8, top + 9, left + w - 8, top + 11, 0xFF333333);
        g.fill(left + 8, top + 9, left + 8 + (w - 16) * nenMeta[2] / 100, top + 11, 0xFF4F9BE0);
        GNode hover = nodeAt(mouseX, mouseY);
        String focus = hover != null ? hover.ability().id() : "";
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
        if (!selected.isEmpty()) return false;
        int w = panelW();
        int left = width / 2 - w / 2;
        if (tab == 3) {
            GNode n = nodeAt(mouseX, mouseY);
            if (n != null) open(n.ability().id());
            else if (mouseY >= 52 && mouseY < 70 && mouseX >= left && mouseX < left + w) open("nen"); // bandeau du niveau de Nen
            return true;
        }
        if (tab == 0 || tab == 4) {
            for (Card c : cards(left, 56, w)) {
                if (mouseX >= c.x() && mouseX < c.x() + c.w() && mouseY >= c.y() && mouseY < c.y() + CARD_H) {
                    open(c.id());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Échap : referme d'abord la fiche ouverte
        if (keyCode == 256 && !selected.isEmpty()) {
            closeDetail();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void open(String id) {
        selected = id;
        scroll = 0;
        rebuildWidgets();
    }

    /** Ouvre la fiche d'une stat, d'une technique ou de l'aura (scénarios de test et raccourcis). */
    public boolean pickSubject(String id) {
        parseDetails();
        boolean text = id.startsWith("txt:");
        if (!text && !subjects.containsKey(id)) return false;
        int target = text || com.evandouard.hxh.stats.ProfileDetails.STATS.contains(id) ? 0
                : com.evandouard.hxh.stats.ProfileDetails.AURA.contains(id) ? 4 : 3;
        tab = target; // lastTab inchangé : le carnet se rouvre sur l'onglet que le joueur a choisi
        selected = id;
        scroll = 0;
        rebuildWidgets();
        return true;
    }

    private void closeDetail() {
        selected = "";
        scroll = 0;
        rebuildWidgets();
    }

    // ---------- Fiches : cartes cliquables (Personnage, Aura) et détail par niveau ----------

    private record Eff(char flag, String key, String value) {}

    private record Milestone(int level, List<Eff> effects) {}

    private static final class Subject {
        boolean ok = true;
        int level, xp, next = -1, cap = 100;
        final List<Milestone> milestones = new ArrayList<>();
    }

    private final java.util.Map<String, Subject> subjects = new java.util.HashMap<>();
    private List<String> parsedFrom;

    private void parseDetails() {
        if (parsedFrom == data.details()) return;
        parsedFrom = data.details();
        subjects.clear();
        for (String line : parsedFrom) {
            try {
                String[] p = line.split("\\|", 4);
                if (p[0].equals("D")) {
                    String[] q = line.split("\\|");
                    Subject sub = subjects.computeIfAbsent(q[1], k -> new Subject());
                    sub.ok = q[2].equals("1");
                    sub.level = Integer.parseInt(q[3]);
                    sub.xp = Integer.parseInt(q[4]);
                    sub.next = Integer.parseInt(q[5]);
                    sub.cap = Integer.parseInt(q[6]);
                } else if (p[0].equals("M")) {
                    Subject sub = subjects.computeIfAbsent(p[1], k -> new Subject());
                    int at = Integer.parseInt(p[2].split("\\|")[0]);
                    List<Eff> effects = new ArrayList<>();
                    if (p.length > 3) for (String token : p[3].split(";")) {
                        if (token.isEmpty()) continue;
                        char flag = token.charAt(0) == '!' || token.charAt(0) == '+' ? token.charAt(0) : ' ';
                        String rest = flag == ' ' ? token : token.substring(1);
                        int eq = rest.indexOf('=');
                        effects.add(new Eff(flag, eq < 0 ? rest : rest.substring(0, eq), eq < 0 ? "" : rest.substring(eq + 1)));
                    }
                    sub.milestones.add(new Milestone(at, effects));
                }
            } catch (RuntimeException ignored) {
                // ligne illisible : on l'ignore plutôt que de casser tout le carnet
            }
        }
    }

    private static final int CARD_H = 26;

    private record Card(String id, Component label, int x, int y, int w) {}

    private List<Card> cards(int left, int top, int w) {
        List<Card> out = new ArrayList<>();
        if (tab == 4) {
            String[] ids = {"aura", "heal", "regen"};
            int cw = (w - 16 - 8) / 3;
            for (int i = 0; i < ids.length; i++) out.add(new Card(ids[i], Component.translatable("screen.hxh.aura." + ids[i]),
                    left + 8 + i * (cw + 4), top + 14, cw));
            return out;
        }
        int cw = (w - 16 - 16) / 5;
        for (int i = 0; i < com.evandouard.hxh.stats.ProfileDetails.STATS.size(); i++) {
            String id = com.evandouard.hxh.stats.ProfileDetails.STATS.get(i);
            out.add(new Card(id, Component.translatable("stat.hxh." + id), left + 8 + i * (cw + 4), top + 12, cw));
        }
        String[] texts = {"summary", "nen_identity", "family"};
        String[] ids = {"txt:summary", "txt:identity", "txt:family"};
        int tw = (w - 16 - 8) / 3;
        for (int i = 0; i < texts.length; i++) out.add(new Card(ids[i], Component.translatable("screen.hxh.character." + texts[i]),
                left + 8 + i * (tw + 4), top + 44, tw));
        return out;
    }

    private void renderCards(GuiGraphics g, int left, int top, int w, int mouseX, int mouseY) {
        if (tab == 0) {
            g.drawString(font, data.character().isEmpty() ? Component.empty() : data.character().get(0), left + 8, top - 1, 0xFFD27F);
        } else {
            g.drawString(font, Component.translatable("screen.hxh.aura.title"), left + 8, top - 1, 0x9FD3FF);
        }
        for (Card c : cards(left, top, w)) {
            Subject sub = subjects.get(c.id());
            boolean hover = mouseX >= c.x() && mouseX < c.x() + c.w() && mouseY >= c.y() && mouseY < c.y() + CARD_H;
            boolean locked = sub != null && !sub.ok;
            g.fill(c.x() - 1, c.y() - 1, c.x() + c.w() + 1, c.y() + CARD_H + 1, hover ? 0xFFFFFFFF : locked ? 0xFF555555 : 0xFF8C6A2E);
            g.fill(c.x(), c.y(), c.x() + c.w(), c.y() + CARD_H, 0xFF222222);
            g.drawCenteredString(font, c.label(), c.x() + c.w() / 2, c.y() + 3, locked ? 0xFF999999 : 0xFFFFFF);
            if (sub != null) {
                g.drawCenteredString(font, Component.translatable("screen.hxh.detail.level", sub.level), c.x() + c.w() / 2, c.y() + 13, 0xFFFFD27F);
                if (sub.next > 0) {
                    g.fill(c.x(), c.y() + CARD_H - 3, c.x() + c.w(), c.y() + CARD_H, 0xFF333333);
                    g.fill(c.x(), c.y() + CARD_H - 3, c.x() + Math.min(c.w(), c.w() * sub.xp / sub.next), c.y() + CARD_H, 0xFF4F9BE0);
                }
            }
        }
        if (tab == 4) {
            int y = top + 50;
            for (FormattedCharSequence l : wrapped()) {
                if (y + 9 > panelBottom()) break;
                g.drawString(font, l, left + 8, y, 0xFFFFFF);
                y += 11;
            }
        }
    }

    private Component subjectName(String id) {
        return switch (id) {
            case "txt:summary" -> Component.translatable("screen.hxh.character.summary");
            case "txt:identity" -> Component.translatable("screen.hxh.character.nen_identity");
            case "txt:family" -> Component.translatable("screen.hxh.character.family");
            case "nen" -> Component.translatable("screen.hxh.character.nen_level");
            case "aura", "heal", "regen" -> Component.translatable("screen.hxh.aura." + id);
            case "strength", "speed", "endurance", "vitality", "willpower" -> Component.translatable("stat.hxh." + id);
            default -> Component.translatable("ability.hxh." + id);
        };
    }

    private Component effectText(Eff e) {
        Component arg = e.value().startsWith("@") ? Component.translatable(e.value().substring(1)) : Component.literal(e.value());
        net.minecraft.network.chat.MutableComponent text = Component.translatable("detail.hxh.eff." + e.key(), arg);
        if (e.flag() == '!') return Component.translatable("screen.hxh.detail.tag_eff").withStyle(net.minecraft.ChatFormatting.GREEN)
                .append(text.withStyle(net.minecraft.ChatFormatting.WHITE));
        if (e.flag() == '+') return Component.translatable("screen.hxh.detail.tag_unlock").withStyle(net.minecraft.ChatFormatting.GOLD)
                .append(text.withStyle(net.minecraft.ChatFormatting.WHITE));
        return text.withStyle(net.minecraft.ChatFormatting.WHITE);
    }

    /** Fiche d'une stat, d'une technique ou de l'aura : description, XP, barre de paliers et effets du palier visé. */
    private void renderDetail(GuiGraphics g, int left, int top, int w, int bottom, int mouseX, int mouseY) {
        int x = left + 8, right = left + w - 8;
        g.drawString(font, subjectName(selected), x, top, 0xFFD27F);
        if (selected.startsWith("txt:")) {
            List<FormattedCharSequence> rows = wrapped();
            int y = top + 14;
            for (int i = scroll; i < rows.size() && y + 9 <= bottom; i++) {
                g.drawString(font, rows.get(i), x, y, 0xFFFFFF);
                y += 11;
            }
            return;
        }
        Subject sub = subjects.get(selected);
        if (sub == null) return;
        int y = top + 11;
        Component level = sub.cap < MAX_LEVEL_BAR && sub.cap > 0
                ? Component.translatable("screen.hxh.detail.level_cap", sub.level, sub.cap)
                : Component.translatable("screen.hxh.detail.level", sub.level);
        g.drawString(font, level, x, y, 0xFFFFFFFF);
        if (!sub.ok) g.drawString(font, Component.translatable("screen.hxh.detail.locked"), x + font.width(level) + 8, y, 0xFFE05050);
        y += 11;
        // description
        int maxDesc = Math.max(1, (bottom - y - 78) / 10);
        List<FormattedCharSequence> desc = font.split(Component.translatable("detail.hxh.desc." + selected), right - x);
        for (int i = 0; i < Math.min(maxDesc, desc.size()); i++) {
            g.drawString(font, desc.get(i), x, y, 0xFFBBBBBB);
            y += 10;
        }
        // conditions de déblocage (graphe du Nen) tant que la technique n'est pas acquise
        GNode node = graphNodes.get(selected);
        if (node != null && !node.learned()) {
            StringBuilder sb = new StringBuilder(Component.translatable("screen.hxh.detail.reqs").getString());
            if (node.reqs().isEmpty()) sb.append(Component.translatable("screen.hxh.nen_graph.base").getString());
            boolean first = true;
            for (String[] r : node.reqs()) {
                if (!first) sb.append("  ");
                first = false;
                boolean done = Integer.parseInt(r[2]) >= Integer.parseInt(r[1]);
                if (r[0].equals("hatsu")) sb.append(done ? "§a" : "§7").append(Component.translatable("screen.hxh.nen_graph.hatsu").getString());
                else sb.append(done ? "§a" : "§7").append(Component.translatable("mastery.hxh." + r[0]).getString())
                        .append(' ').append(Math.min(Integer.parseInt(r[2]), 999)).append('/').append(r[1]);
                sb.append("§r");
            }
            for (FormattedCharSequence l : font.split(Component.literal(sb.toString()), right - x)) {
                g.drawString(font, l, x, y, 0xFFFFFF);
                y += 10;
            }
        }
        y += 2;
        // XP
        if (sub.next > 0) {
            Component xp = Component.translatable("screen.hxh.detail.xp", sub.xp, sub.next);
            g.drawString(font, xp, x, y, 0xFF9FD3FF);
            int bx = x + font.width(xp) + 6;
            g.fill(bx, y + 2, right, y + 6, 0xFF333333);
            g.fill(bx, y + 2, bx + (right - bx) * Math.min(sub.xp, sub.next) / sub.next, y + 6, 0xFF4F9BE0);
        } else {
            g.drawString(font, Component.translatable("screen.hxh.detail.max"), x, y, 0xFF9FD3FF);
        }
        y += 12;
        // barre de paliers : niveau 0 à 100, un repère par palier
        int barL = x + 6, barR = right - 6, barW = barR - barL;
        g.fill(barL, y + 3, barR, y + 6, 0xFF333333);
        g.fill(barL, y + 3, barL + barW * Math.min(100, sub.level) / 100, y + 6, 0xFF4FBF6A);
        if (sub.cap < 100) g.fill(barL + barW * sub.cap / 100, y, barL + barW * sub.cap / 100 + 1, y + 9, 0xFFE05050);
        int hovered = -1, best = 99;
        for (int i = 0; i < sub.milestones.size(); i++) {
            int mx = barL + barW * sub.milestones.get(i).level() / 100;
            int d = Math.abs(mouseX - mx);
            if (d <= 5 && mouseY >= y - 3 && mouseY <= y + 22 && d < best) {
                best = d;
                hovered = i;
            }
        }
        int lastRight = -100, reached = 0;
        for (int i = 0; i < sub.milestones.size(); i++) {
            Milestone m = sub.milestones.get(i);
            int mx = barL + barW * m.level() / 100;
            boolean done = m.level() <= sub.level;
            if (done) reached = i;
            int color = done ? 0xFF5FBF6A : m.level() <= sub.cap ? 0xFF999999 : 0xFF774444;
            if (i == hovered) g.fill(mx - 3, y - 1, mx + 4, y + 10, 0xFFFFFFFF);
            g.fill(mx - 2, y, mx + 3, y + 9, color);
            String label = String.valueOf(m.level());
            int lw = font.width(label);
            if (mx - lw / 2 > lastRight + 3) {
                g.drawString(font, label, mx - lw / 2, y + 11, done ? 0xFFFFFFFF : 0xFF888888);
                lastRight = mx + lw / 2;
            }
        }
        y += 23;
        // ce que fait le palier visé (ou le dernier atteint)
        if (sub.milestones.isEmpty()) return;
        Milestone shown = sub.milestones.get(hovered >= 0 ? hovered : reached);
        net.minecraft.network.chat.MutableComponent head = hovered >= 0
                ? Component.translatable("screen.hxh.detail.at", shown.level())
                : Component.translatable("screen.hxh.detail.now", shown.level());
        if (hovered >= 0 && shown.level() > sub.cap && sub.cap < 100) head.append(Component.translatable("screen.hxh.detail.beyond_cap"));
        g.drawString(font, head.withStyle(net.minecraft.ChatFormatting.AQUA), x, y, 0xFFFFFFFF);
        y += 11;
        if (shown.effects().isEmpty()) {
            g.drawString(font, Component.translatable("screen.hxh.detail.hint"), x, y, 0xFF888888);
            return;
        }
        for (Eff e : shown.effects()) {
            for (FormattedCharSequence l : font.split(effectText(e), right - x)) {
                if (y + 9 > bottom) return;
                g.drawString(font, l, x, y, 0xFFFFFFFF);
                y += 10;
            }
        }
    }

    private static final int MAX_LEVEL_BAR = 100;

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
