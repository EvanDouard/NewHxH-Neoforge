package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.hatsu.HatsuKit;
import com.evandouard.hxh.hatsu.Passive;
import com.evandouard.hxh.hatsu.Technique;
import com.evandouard.hxh.hatsu.Techniques;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fiche d'un personnage : passifs puis chaque technique avec sa description, sa catégorie,
 * l'affinité pour le joueur, le coût, la recharge et les conditions. Défilable à la molette.
 */
public class TechniqueListScreen extends Screen {
    private static final int MARGIN = 16;
    private static final int TOP = 30;

    private final HatsuKit kit;
    @Nullable
    private final Screen parent;
    private final List<Line> lines = new ArrayList<>();
    private double scroll = 0;

    private record Line(FormattedCharSequence text, int indent, boolean gapBefore) {}

    public TechniqueListScreen(HatsuKit kit, @Nullable Screen parent) {
        super(Component.translatable(kit.translationKey()));
        this.kit = kit;
        this.parent = parent;
    }

    @Override
    protected void init() {
        buildLines();
        addRenderableWidget(Button.builder(parent != null ? CommonComponents.GUI_BACK : CommonComponents.GUI_DONE,
                b -> onClose()).bounds(width / 2 - 50, height - 26, 100, 20).build());
        // Sa propre fiche : accès aux serments et restrictions
        HunterData mine = Minecraft.getInstance().player.getData(ModAttachments.HUNTER);
        if (kit.id().equals(mine.kit())) {
            addRenderableWidget(Button.builder(Component.translatable("screen.hxh.vow.open"),
                    b -> minecraft.setScreen(new VowScreen(this))).bounds(width - 126, height - 26, 110, 20).build());
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void add(Component text, int indent, boolean gap) {
        int wrap = width - 2 * MARGIN - indent - 8;
        boolean first = true;
        for (FormattedCharSequence seq : font.split(text, wrap)) {
            lines.add(new Line(seq, indent, gap && first));
            first = false;
        }
    }

    private void buildLines() {
        lines.clear();
        HunterData data = Minecraft.getInstance().player.getData(ModAttachments.HUNTER);
        NenType viewerType = data.nenType();

        MutableComponent typeLine = Component.translatable("screen.hxh.techniques.type",
                Component.translatable(kit.type().translationKey()));
        if (!kit.typeConfirmed()) typeLine.append(Component.translatable("screen.hxh.hatsu.type_guess"));
        add(typeLine.withStyle(ChatFormatting.AQUA), 0, false);
        add(Component.translatable(kit.descriptionKey()).withStyle(ChatFormatting.GRAY), 0, false);
        if (!kit.implemented()) add(Component.translatable("screen.hxh.techniques.idea_notice").withStyle(ChatFormatting.LIGHT_PURPLE), 0, false);

        for (Passive passive : kit.passives()) {
            add(Component.translatable("screen.hxh.techniques.passive", Component.translatable(passive.translationKey()))
                    .withStyle(ChatFormatting.GREEN), 0, true);
            add(Component.translatable(passive.descriptionKey()), 8, false);
        }

        int n = 1;
        for (String id : kit.techniques()) {
            Technique t = Techniques.get(id);
            if (t == null) continue;
            add(Component.literal(n++ + ". ").append(Component.translatable(t.translationKey()))
                    .withStyle(ChatFormatting.GOLD), 0, true);
            add(Component.translatable(t.descriptionKey()), 8, false);

            MutableComponent category = Component.translatable("screen.hxh.techniques.category", NenBadge.label(t));
            if (viewerType != null || com.evandouard.hxh.hatsu.NenAffinity.physical(t)) {
                category.append(Component.translatable("screen.hxh.techniques.affinity", Math.round(
                        com.evandouard.hxh.hatsu.NenAffinity.of(net.minecraft.client.Minecraft.getInstance().player, data, t) * 100)));
            }
            add(category.withStyle(ChatFormatting.GRAY), 8, false);
            if (t.minNenLevel() > 0) {
                add(Component.translatable("screen.hxh.techniques.min_level", t.minNenLevel())
                        .withStyle(data.nenLevel() >= t.minNenLevel() ? ChatFormatting.GRAY : ChatFormatting.RED), 8, false);
            }
            if (t.implemented()) {
                add(Component.translatable("screen.hxh.techniques.stats", t.auraCost(),
                        String.format(Locale.ROOT, "%.1f", t.cooldownTicks() / 20f)).withStyle(ChatFormatting.GRAY), 8, false);
            }
            add(Component.translatable("screen.hxh.techniques.conditions", t.conditions()).withStyle(ChatFormatting.GRAY), 8, false);
        }
    }

    private int contentHeight() {
        int h = 0;
        for (Line l : lines) h += (l.gapBefore() ? 6 : 0) + font.lineHeight + 1;
        return h;
    }

    private int viewHeight() {
        return height - TOP - 34;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, contentHeight() - viewHeight());
        scroll = Math.max(0, Math.min(max, scroll - scrollY * 12));
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title.copy().withStyle(ChatFormatting.GOLD), width / 2, 12, 0xFFFFFF);

        int top = TOP;
        int bottom = top + viewHeight();
        graphics.fill(MARGIN - 4, top - 4, width - MARGIN + 4, bottom + 4, 0x80000000);
        graphics.enableScissor(MARGIN - 4, top - 2, width - MARGIN + 4, bottom + 2);
        int y = top - (int) scroll;
        for (Line l : lines) {
            if (l.gapBefore()) y += 6;
            if (y > top - font.lineHeight && y < bottom) graphics.drawString(font, l.text(), MARGIN + l.indent(), y, 0xFFFFFF, false);
            y += font.lineHeight + 1;
        }
        graphics.disableScissor();
        if (contentHeight() > viewHeight()) {
            graphics.drawString(font, Component.translatable("screen.hxh.techniques.scroll"), width - MARGIN - 70, height - 20, 0xFF808080, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
