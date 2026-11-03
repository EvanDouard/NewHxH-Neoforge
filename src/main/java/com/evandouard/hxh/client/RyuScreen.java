package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.network.SetRyuPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Réglage du Ryu : quelle part de l'aura va dans l'attaque (le reste protège le corps).
 * Appliqué en continu ; plus la maîtrise du Ryu est haute, plus la répartition a d'effet.
 */
public class RyuScreen extends Screen {
    private int attack;

    public RyuScreen() {
        super(Component.translatable("screen.hxh.ryu.title"));
    }

    @Override
    protected void init() {
        HunterData data = Minecraft.getInstance().player.getData(ModAttachments.HUNTER);
        attack = data.ryuAttack();
        addRenderableWidget(new AbstractSliderButton(width / 2 - 100, height / 2 - 10, 200, 20,
                label(attack), attack / 100.0) {
            @Override
            protected void updateMessage() {
                setMessage(label((int) Math.round(value * 100)));
            }

            @Override
            protected void applyValue() {
                attack = (int) Math.round(value * 100);
            }
        });
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 50, height / 2 + 30, 100, 20).build());
    }

    private static Component label(int attackPercent) {
        return Component.translatable("screen.hxh.ryu.slider", attackPercent, 100 - attackPercent);
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new SetRyuPayload(attack));
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 50, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.hxh.ryu.help").withStyle(ChatFormatting.GRAY),
                width / 2, height / 2 - 34, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
