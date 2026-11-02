package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.hatsu.HatsuKit;
import com.evandouard.hxh.hatsu.HatsuKits;
import com.evandouard.hxh.hatsu.Technique;
import com.evandouard.hxh.hatsu.Techniques;
import com.evandouard.hxh.network.ChooseKitPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Écran de choix du Hatsu : tous les personnages emblématiques et leurs techniques.
 * Seuls ceux du type de Nen du joueur peuvent être choisis, et le choix est définitif.
 */
public class HatsuSelectionScreen extends Screen {
    private static final int ROW_HEIGHT = 34;
    private static final int BUTTON_WIDTH = 110;

    public HatsuSelectionScreen() {
        super(Component.translatable("screen.hxh.hatsu.title"));
    }

    private HunterData data() {
        return Minecraft.getInstance().player.getData(ModAttachments.HUNTER);
    }

    @Override
    protected void init() {
        List<HatsuKit> kits = HatsuKits.all();
        int top = 40;
        int right = width - 20;
        for (int i = 0; i < kits.size(); i++) {
            HatsuKit kit = kits.get(i);
            String refusal = Hatsu.chooseRefusal(data(), kit);
            Component name = Component.translatable(kit.translationKey());
            Button button = Button.builder(Component.translatable("screen.hxh.hatsu.choose", name), b -> confirm(kit))
                    .bounds(right - BUTTON_WIDTH, top + i * ROW_HEIGHT + 6, BUTTON_WIDTH, 20)
                    .build();
            button.active = refusal == null;
            if (refusal != null) {
                Component reason = kit.type() != data().nenType() && "message.hxh.hatsu_wrong_type".equals(refusal)
                        ? Component.translatable("screen.hxh.hatsu.reserved", Component.translatable(kit.type().translationKey()))
                        : Component.translatable(refusal);
                button.setTooltip(Tooltip.create(reason));
            }
            addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
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
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        HunterData data = data();
        Component typeLine = data.nenType() == null
                ? Component.translatable("nen.hxh.none")
                : Component.translatable(data.nenType().translationKey());
        graphics.drawCenteredString(font, Component.translatable("screen.hxh.hatsu.your_type", typeLine)
                .withStyle(ChatFormatting.GRAY), width / 2, 24, 0xFFFFFF);

        List<HatsuKit> kits = HatsuKits.all();
        int top = 40;
        int left = 20;
        for (int i = 0; i < kits.size(); i++) {
            HatsuKit kit = kits.get(i);
            int y = top + i * ROW_HEIGHT;
            boolean mine = kit.type() == data.nenType();
            graphics.fill(left - 4, y, width - 16, y + ROW_HEIGHT - 2, mine ? 0x60305070 : 0x40000000);

            MutableComponent header = Component.translatable(kit.translationKey()).withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("  "))
                    .append(Component.translatable(kit.type().translationKey())
                            .withStyle(mine ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
            graphics.drawString(font, header, left, y + 4, 0xFFFFFF);

            MutableComponent techniques = Component.empty();
            for (int t = 0; t < kit.techniques().size(); t++) {
                Technique technique = Techniques.get(kit.techniques().get(t));
                if (technique == null) continue;
                if (t > 0) techniques.append(Component.literal(" · "));
                techniques.append(Component.translatable("screen.hxh.hatsu.technique",
                        Component.translatable(technique.translationKey()), technique.auraCost()));
            }
            graphics.drawString(font, techniques.withStyle(ChatFormatting.GRAY), left, y + 17, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
