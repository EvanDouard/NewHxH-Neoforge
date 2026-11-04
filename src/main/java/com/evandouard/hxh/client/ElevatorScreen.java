package com.evandouard.hxh.client;

import com.evandouard.hxh.network.ElevatorGoPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Ascenseur de la Tour Céleste : hall d'entrée, sa chambre, et les halls des tranches d'étages
 * jusqu'à la sienne. Un clic = on y va.
 */
public class ElevatorScreen extends Screen {
    private static final int COLUMNS = 4;
    private static final int BUTTON_W = 70;
    private final List<String> brackets;
    private final int current;
    private final boolean room;

    public ElevatorScreen(List<String> brackets, int current, boolean room) {
        super(Component.translatable("screen.hxh.elevator.title"));
        this.brackets = brackets;
        this.current = current;
        this.room = room;
    }

    @Override
    protected void init() {
        int left = width / 2 - (COLUMNS * (BUTTON_W + 4) - 4) / 2;
        int y = height / 2 - 60;
        addDestination(Component.translatable("screen.hxh.elevator.lobby"), 0, left, y, BUTTON_W * 2 + 4);
        if (room) addDestination(Component.translatable("screen.hxh.elevator.room"), -1, left + 2 * (BUTTON_W + 4), y, BUTTON_W * 2 + 4);
        y += 30;
        for (int i = 0; i < brackets.size(); i++) {
            int col = i % COLUMNS, row = i / COLUMNS;
            addDestination(Component.literal(brackets.get(i)), i + 1, left + col * (BUTTON_W + 4), y + row * 24, BUTTON_W);
        }
        int rows = (brackets.size() + COLUMNS - 1) / COLUMNS;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(width / 2 - 50, y + rows * 24 + 8, 100, 20).build());
    }

    private void addDestination(Component label, int destination, int x, int y, int w) {
        Button b = addRenderableWidget(Button.builder(label, btn -> {
            PacketDistributor.sendToServer(new ElevatorGoPayload(destination));
            onClose();
        }).bounds(x, y, w, 20).build());
        b.active = destination != current;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 90, 0xFFD27F);
        graphics.drawCenteredString(font, Component.translatable("screen.hxh.elevator.hint"), width / 2, height / 2 - 76, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
