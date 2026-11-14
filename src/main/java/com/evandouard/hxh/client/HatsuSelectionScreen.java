package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.hatsu.HatsuKit;
import com.evandouard.hxh.hatsu.HatsuKits;
import com.evandouard.hxh.network.ChooseKitPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Écran de choix du Hatsu : tous les personnages, paginés. Chaque ligne a « Voir » (fiche détaillée
 * des techniques et passifs) et « Choisir » (seulement pour son type, choix définitif).
 * Un filtre permet de n'afficher que les personnages de son type.
 */
public class HatsuSelectionScreen extends Screen {
    private static final int ROW_HEIGHT = 26;
    private static final int TOP = 36;
    private static final int PER_PAGE = 6;

    private int page = 0;
    private boolean onlyMyType = false;
    /** Filtre par type de Nen (pastilles en haut à droite), null = tous. */
    @org.jetbrains.annotations.Nullable
    private com.evandouard.hxh.data.NenType typeFilter = null;

    public HatsuSelectionScreen() {
        super(Component.translatable("screen.hxh.hatsu.title"));
    }

    private static HunterData data() {
        return Minecraft.getInstance().player.getData(ModAttachments.HUNTER);
    }

    private List<HatsuKit> visibleKits() {
        HunterData d = data();
        return HatsuKits.all().stream().filter(k -> !onlyMyType || k.type() == d.nenType())
                .filter(k -> typeFilter == null || k.type() == typeFilter).toList();
    }

    private int pageCount() {
        return Math.max(1, (visibleKits().size() + PER_PAGE - 1) / PER_PAGE);
    }

    @Override
    protected void init() {
        List<HatsuKit> kits = visibleKits();
        page = Math.min(page, pageCount() - 1);
        int right = width - 16;
        for (int row = 0; row < PER_PAGE; row++) {
            int index = page * PER_PAGE + row;
            if (index >= kits.size()) break;
            HatsuKit kit = kits.get(index);
            int y = TOP + row * ROW_HEIGHT + 3;

            String refusal = Hatsu.chooseRefusal(data(), kit, minecraft.player != null && minecraft.player.isCreative());
            Button choose = Button.builder(Component.translatable("screen.hxh.hatsu.choose_short"), b -> confirm(kit))
                    .bounds(right - 60, y, 60, 20).build();
            choose.active = refusal == null;
            choose.setTooltip(Tooltip.create(refusal == null
                    ? Component.translatable("screen.hxh.hatsu.choose", Component.translatable(kit.translationKey()))
                    : reason(kit, refusal)));
            addRenderableWidget(choose);

            addRenderableWidget(Button.builder(Component.translatable("screen.hxh.hatsu.view"),
                            b -> minecraft.setScreen(new TechniqueListScreen(kit, this)))
                    .bounds(right - 60 - 4 - 46, y, 46, 20).build());
        }

        int bottom = height - 26;
        Button prev = Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); })
                .bounds(width / 2 - 70, bottom, 20, 20).build();
        prev.active = page > 0;
        addRenderableWidget(prev);
        Button next = Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); })
                .bounds(width / 2 + 50, bottom, 20, 20).build();
        next.active = page < pageCount() - 1;
        addRenderableWidget(next);
        addRenderableWidget(Button.builder(Component.translatable(onlyMyType
                                ? "screen.hxh.hatsu.filter_mine" : "screen.hxh.hatsu.filter_all"),
                        b -> { onlyMyType = !onlyMyType; typeFilter = null; page = 0; rebuildWidgets(); })
                .bounds(16, bottom, 110, 20).build());
        // Pastilles des six types : un clic trie la liste sur ce type, un second clic enlève le filtre
        com.evandouard.hxh.data.NenType[] types = com.evandouard.hxh.data.NenType.values();
        int bx = width - 16 - types.length * (TypeButton.SIZE + 2);
        for (com.evandouard.hxh.data.NenType type : types) {
            addRenderableWidget(new TypeButton(bx, 6, type, type == typeFilter, () -> {
                typeFilter = typeFilter == type ? null : type;
                onlyMyType = false;
                page = 0;
                rebuildWidgets();
            }));
            bx += TypeButton.SIZE + 2;
        }
        // Serments et restrictions (une fois le personnage choisi)
        Button vows = Button.builder(Component.translatable("screen.hxh.vow.open"), b -> minecraft.setScreen(new VowScreen(this)))
                .bounds(width - 126, bottom, 110, 20).build();
        vows.active = data().hasKit();
        addRenderableWidget(vows);
    }

    /** Bouton rond d'un type de Nen : la pastille des techniques, cadre doré quand le filtre est actif. */
    private static final class TypeButton extends net.minecraft.client.gui.components.AbstractButton {
        static final int SIZE = 16;
        private final com.evandouard.hxh.data.NenType type;
        private final boolean on;
        private final Runnable action;

        TypeButton(int x, int y, com.evandouard.hxh.data.NenType type, boolean on, Runnable action) {
            super(x, y, SIZE, SIZE, Component.translatable(type.translationKey()));
            this.type = type;
            this.on = on;
            this.action = action;
            // info-bulle : le nom dans la langue du jeu, et en anglais
            String english = com.evandouard.hxh.hatsu.NenAffinity.englishName(type);
            MutableComponent tip = Component.translatable(type.translationKey()).copy().withStyle(s ->
                    s.withColor(com.evandouard.hxh.hatsu.NenAffinity.color(type) & 0xFFFFFF).withBold(true));
            if (!Component.translatable(type.translationKey()).getString().equals(english)) {
                tip.append(Component.literal(" (" + english + ")").withStyle(ChatFormatting.GRAY));
            }
            tip.append(Component.literal("\n")).append(Component.translatable(on ? "screen.hxh.hatsu.filter_type_off"
                    : "screen.hxh.hatsu.filter_type_on").withStyle(ChatFormatting.DARK_GRAY));
            setTooltip(Tooltip.create(tip));
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int frame = on ? 0xFFFFD23F : isHoveredOrFocused() ? 0xFFFFFFFF : 0x00000000;
            if (frame != 0) {
                g.fill(getX() - 1, getY() - 1, getX() + SIZE + 1, getY(), frame);
                g.fill(getX() - 1, getY() + SIZE, getX() + SIZE + 1, getY() + SIZE + 1, frame);
                g.fill(getX() - 1, getY(), getX(), getY() + SIZE, frame);
                g.fill(getX() + SIZE, getY(), getX() + SIZE + 1, getY() + SIZE, frame);
            }
            var pose = g.pose();
            pose.pushPose();
            float k = SIZE / (float) NenBadge.SIZE;
            pose.translate(getX(), getY(), 0);
            pose.scale(k, k, 1);
            NenBadge.draw(g, Minecraft.getInstance().font, type, 0, 0);
            pose.popPose();
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out) {
            defaultButtonNarrationText(out);
        }
    }

    private static Component reason(HatsuKit kit, String refusal) {
        if ("message.hxh.hatsu_wrong_type".equals(refusal)) {
            return Component.translatable("screen.hxh.hatsu.reserved", Component.translatable(kit.type().translationKey()));
        }
        return Component.translatable(refusal);
    }

    private void confirm(HatsuKit kit) {
        Component name = Component.translatable(kit.translationKey());
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                PacketDistributor.sendToServer(new ChooseKitPayload(kit.id()));
                minecraft.setScreen(null);
            } else {
                minecraft.setScreen(this);
            }
        }, Component.translatable("screen.hxh.hatsu.confirm_title", name),
                Component.translatable("screen.hxh.hatsu.confirm_text")));
    }

    /** Fond + textes dessinés sous les boutons. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        HunterData data = data();
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        Component typeLine = data.nenType() == null
                ? Component.translatable("nen.hxh.none")
                : Component.translatable(data.nenType().translationKey());
        graphics.drawCenteredString(font, Component.translatable("screen.hxh.hatsu.your_type", typeLine)
                .withStyle(ChatFormatting.GRAY), width / 2, 22, 0xFFFFFF);

        List<HatsuKit> kits = visibleKits();
        for (int row = 0; row < PER_PAGE; row++) {
            int index = page * PER_PAGE + row;
            if (index >= kits.size()) break;
            HatsuKit kit = kits.get(index);
            int y = TOP + row * ROW_HEIGHT;
            boolean mine = kit.type() == data.nenType();
            graphics.fill(12, y, width - 12, y + ROW_HEIGHT - 2, mine ? 0x60305070 : 0x40000000);

            MutableComponent header = Component.translatable(kit.translationKey())
                    .withStyle(kit.implemented() ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
            graphics.drawString(font, header, 18, y + 3, 0xFFFFFF);

            MutableComponent sub = Component.translatable(kit.type().translationKey())
                    .withStyle(mine ? ChatFormatting.AQUA : ChatFormatting.GRAY);
            if (!kit.typeConfirmed()) sub.append(Component.translatable("screen.hxh.hatsu.type_guess").withStyle(ChatFormatting.DARK_GRAY));
            if (!kit.implemented()) sub.append(Component.translatable("screen.hxh.hatsu.idea_tag").withStyle(ChatFormatting.LIGHT_PURPLE));
            NenBadge.draw(graphics, font, kit.type(), 18, y + 12);
            graphics.drawString(font, sub, 18 + NenBadge.SIZE + 3, y + 13, 0xFFFFFF);
        }
        graphics.drawCenteredString(font, Component.literal((page + 1) + " / " + pageCount()),
                width / 2, height - 20, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
