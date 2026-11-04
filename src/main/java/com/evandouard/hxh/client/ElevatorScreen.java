package com.evandouard.hxh.client;

import com.evandouard.hxh.network.ElevatorGoPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ascenseur de la Tour Céleste : choisir un étage entre le hall (0) et son étage de combat.
 */
public class ElevatorScreen extends Screen {
    private final int maxFloor;
    private final int currentFloor;
    private int selected;
    private Button go;

    public ElevatorScreen(int maxFloor, int currentFloor) {
        super(Component.translatable("screen.hxh.elevator.title"));
        this.maxFloor = maxFloor;
        this.currentFloor = currentFloor;
        // Par défaut : son étage de combat (ou le hall si on y est déjà)
        this.selected = currentFloor == maxFloor ? 0 : maxFloor;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 10;
        int[] steps = {-10, -1, 1, 10};
        String[] labels = {"−10", "−1", "+1", "+10"};
        for (int i = 0; i < steps.length; i++) {
            int step = steps[i];
            addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> select(selected + step))
                    .bounds(cx - 102 + i * 52, y, 48, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.hxh.elevator.lobby"), b -> select(0))
                .bounds(cx - 102, y + 26, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.hxh.elevator.my_floor"), b -> select(maxFloor))
                .bounds(cx + 2, y + 26, 100, 20).build());
        go = addRenderableWidget(Button.builder(Component.translatable("screen.hxh.elevator.go"), b -> {
            PacketDistributor.sendToServer(new ElevatorGoPayload(selected));
            onClose();
        }).bounds(cx - 102, y + 56, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(cx + 2, y + 56, 100, 20).build());
        select(selected);
    }

    private void select(int floor) {
        selected = Math.max(0, Math.min(maxFloor, floor));
        if (go != null) go.active = selected != currentFloor;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int y = height / 2 - 70;
        graphics.drawCenteredString(font, title, cx, y, 0xFFD27F);
        Component floor = selected == 0 ? Component.translatable("screen.hxh.elevator.selected_lobby")
                : Component.translatable("screen.hxh.elevator.selected", selected);
        graphics.drawCenteredString(font, floor.copy().withStyle(ChatFormatting.BOLD), cx, y + 22, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.hxh.elevator.info", currentFloor, maxFloor),
                cx, y + 38, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
