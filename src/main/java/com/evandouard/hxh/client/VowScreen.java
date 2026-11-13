package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.hatsu.Vows;
import com.evandouard.hxh.network.VowPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Serments et restrictions : la liste des serments possibles, leur prix et leur gain. Un seul à la fois ; y renoncer
 * revient à le rompre (Hatsu scellé). Le survol d'une ligne affiche sa description en bas.
 */
public class VowScreen extends Screen {
    private static final int ROW = 22;
    private static final int TOP = 44;
    @Nullable
    private final Screen parent;
    private int hovered = -1;

    public VowScreen(@Nullable Screen parent) {
        super(Component.translatable("screen.hxh.vow.title"));
        this.parent = parent;
    }

    /** Disposition : une colonne si tout tient, sinon deux. */
    private int perColumn() {
        return Math.max(1, (height - TOP - 54) / ROW);
    }

    private int columns(int count) {
        return count > perColumn() ? 2 : 1;
    }

    private int cellW(int count) {
        return columns(count) == 1 ? 300 : 200;
    }

    private int cellX(int i, int count) {
        int cols = columns(count), w = cellW(count);
        int total = cols * w + (cols - 1) * 6;
        return width / 2 - total / 2 + (i / perColumn()) * (w + 6);
    }

    private int cellY(int i) {
        return TOP + (i % perColumn()) * ROW;
    }

    private int buttonW(int count) {
        return columns(count) == 1 ? 84 : 52;
    }

    private static HunterData data() {
        return Minecraft.getInstance().player.getData(ModAttachments.HUNTER);
    }

    @Override
    protected void init() {
        HunterData d = data();
        long now = minecraft.level.getGameTime();
        boolean sealed = Vows.sealed(d, now);
        Vows.Vow current = Vows.of(d);
        Vows.Vow[] vows = shown(d);
        int bw = buttonW(vows.length), cw = cellW(vows.length);
        for (int i = 0; i < vows.length; i++) {
            Vows.Vow v = vows[i];
            int y = cellY(i);
            Button take = Button.builder(Component.translatable(current == v ? "screen.hxh.vow.current" : "screen.hxh.vow.take"),
                    b -> confirmTake(v)).bounds(cellX(i, vows.length) + cw - bw, y + 1, bw, 18).build();
            take.active = current == null && !sealed && d.hasKit();
            take.setTooltip(Tooltip.create(description(d, v)));
            addRenderableWidget(take);
        }
        int bottom = height - 26;
        Button renounce = Button.builder(Component.translatable("screen.hxh.vow.renounce"), b -> confirmRenounce())
                .bounds(width / 2 - 154, bottom, 150, 20).build();
        renounce.active = current != null;
        addRenderableWidget(renounce);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 + 4, bottom, 150, 20).build());
        // Serment à condition : on peut le mettre en pause et le reprendre
        if (current != null && Vows.toggleable(current)) {
            addRenderableWidget(Button.builder(Component.translatable(d.vowOff() ? "screen.hxh.vow.resume" : "screen.hxh.vow.pause"),
                    b -> {
                        PacketDistributor.sendToServer(new VowPayload("toggle", ""));
                        minecraft.setScreen(null);
                    }).bounds(width / 2 - 75, bottom - 24, 150, 20).build());
        }
    }

    /** Serments affichés : les communs, plus celui du personnage s'il en a un. */
    private static Vows.Vow[] shown(HunterData d) {
        boolean own = com.evandouard.hxh.hatsu.CharacterVows.forKit(d.kit()) != null;
        return java.util.Arrays.stream(Vows.Vow.values()).filter(v -> v != Vows.Vow.CHARACTER || own).toArray(Vows.Vow[]::new);
    }

    private static Component description(HunterData d, Vows.Vow v) {
        var def = v == Vows.Vow.CHARACTER ? com.evandouard.hxh.hatsu.CharacterVows.forKit(d.kit()) : null;
        return Component.translatable(def != null ? def.descriptionKey() : v.descriptionKey());
    }

    private static float power(HunterData d, Vows.Vow v) {
        var def = v == Vows.Vow.CHARACTER ? com.evandouard.hxh.hatsu.CharacterVows.forKit(d.kit()) : null;
        return def != null ? def.power() : v.power;
    }

    private void confirmTake(Vows.Vow v) {
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) PacketDistributor.sendToServer(new VowPayload("take", v.id()));
            minecraft.setScreen(yes ? null : this);
        }, Component.translatable("screen.hxh.vow.confirm_title", Vows.name(data(), v)),
                description(data(), v).copy().append("\n\n").append(Component.translatable("screen.hxh.vow.confirm_text"))));
    }

    private void confirmRenounce() {
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) PacketDistributor.sendToServer(new VowPayload("renounce", ""));
            minecraft.setScreen(yes ? null : this);
        }, Component.translatable("screen.hxh.vow.renounce_title"), Component.translatable("screen.hxh.vow.renounce_text",
                Vows.BREAK_SEAL_TICKS / 1200)));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        HunterData d = data();
        long now = minecraft.level.getGameTime();
        g.drawCenteredString(font, title, width / 2, 10, 0xFFD23F);
        Component status;
        if (Vows.sealed(d, now)) {
            status = Component.translatable("screen.hxh.vow.sealed", Math.max(1, (d.sealedUntil() - now) / 1200 + 1)).withStyle(ChatFormatting.RED);
        } else if (Vows.of(d) != null) {
            Component name = Vows.name(d, Vows.of(d));
            status = d.vowUntil() > 0
                    ? Component.translatable("screen.hxh.vow.status_timed", name, Math.max(0, (d.vowUntil() - now) / 20)).withStyle(ChatFormatting.GOLD)
                    : Component.translatable("screen.hxh.vow.status", name).withStyle(ChatFormatting.GOLD);
        } else if (!d.hasKit()) {
            status = Component.translatable("message.hxh.vow_need_hatsu").withStyle(ChatFormatting.GRAY);
        } else {
            status = Component.translatable("screen.hxh.vow.none").withStyle(ChatFormatting.GRAY);
        }
        g.drawCenteredString(font, status, width / 2, 24, 0xFFFFFF);

        hovered = -1;
        Vows.Vow[] vows = shown(d);
        int count = vows.length, cw = cellW(count), bw = buttonW(count);
        for (int i = 0; i < count; i++) {
            Vows.Vow v = vows[i];
            int x = cellX(i, count), y = cellY(i);
            boolean over = mouseX >= x && mouseX <= x + cw && mouseY >= y && mouseY < y + ROW - 2;
            if (over) hovered = i;
            boolean mine = Vows.of(d) == v;
            g.fill(x, y, x + cw, y + ROW - 2, mine ? 0x80604010 : over ? 0x60303050 : 0x40000000);
            String mult = v == Vows.Vow.OVERTIME ? "☀0.7 ☾1.3" : v == Vows.Vow.DAY_SHIFT ? "☀1.3 ☾0.7" : "×" + power(d, v);
            int multX = x + cw - bw - 6 - font.width(mult);
            // nom coupé avant la colonne du multiplicateur (le survol affiche la description en entier)
            String name = Vows.name(d, v).getString();
            int maxW = multX - x - 10;
            if (font.width(name) > maxW) name = font.plainSubstrByWidth(name, maxW - font.width("…")).stripTrailing() + "…";
            g.drawString(font, Component.literal(name).withStyle(mine ? ChatFormatting.GOLD : ChatFormatting.WHITE), x + 6, y + 6, 0xFFFFFF);
            g.drawString(font, Component.literal(mult).withStyle(ChatFormatting.AQUA), multX, y + 6, 0xFFFFFF);
        }
        if (hovered >= 0) {
            int left = width / 2 - 150;
            List<FormattedCharSequence> lines = font.split(description(d, vows[hovered]).copy().withStyle(ChatFormatting.GRAY), 300);
            int y = TOP + Math.min(count, perColumn()) * ROW + 2;
            for (FormattedCharSequence l : lines) {
                if (y > height - 56) break;
                g.drawString(font, l, left, y, 0xFFFFFF);
                y += font.lineHeight + 1;
            }
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
