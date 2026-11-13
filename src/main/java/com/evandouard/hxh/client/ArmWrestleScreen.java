package com.evandouard.hxh.client;

import com.evandouard.hxh.network.ArmWrestleInputPayload;
import com.evandouard.hxh.network.ArmWrestleStatePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Bras de fer, style manga : une jauge verticale encrée. Bande jaune = la zone de l'adversaire, trait rouge = ton
 * bras ; à droite, la barre de manche. Maintiens clic gauche ou Espace pour pousser. Le serveur envoie l'état ;
 * l'écran ne fait qu'afficher et transmettre « je tiens / je lâche ».
 */
public class ArmWrestleScreen extends Screen {
    private static final int INK = 0xFF111111, PAPER = 0xFFFFFBF0, GOLD = 0xFFFFD23F, RED = 0xFFE23A3A, GREEN = 0xFF52D273;
    private ArmWrestleStatePayload state;
    private boolean mouseDown, keyDown, sent;
    private int closeIn = -1;

    public ArmWrestleScreen(ArmWrestleStatePayload state) {
        super(Component.translatable("armwrestle.hxh.title"));
        this.state = state;
    }

    public void update(ArmWrestleStatePayload s) {
        this.state = s;
        if (s.result() != 0 && closeIn < 0) closeIn = 30;
    }

    private void send() {
        boolean hold = mouseDown || keyDown;
        if (hold != sent) {
            sent = hold;
            PacketDistributor.sendToServer(new ArmWrestleInputPayload(hold));
        }
    }

    @Override
    public void tick() {
        if (closeIn > 0 && --closeIn == 0) onClose();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) {
            mouseDown = true;
            send();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0) {
            mouseDown = false;
            send();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            keyDown = true;
            send();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            keyDown = false;
            send();
            return true;
        }
        return super.keyReleased(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int h = Math.min(180, height - 60), w = 34;
        int x = width / 2 - w / 2 - 14, y = height / 2 - h / 2;
        MangaHud.panel(g, x - 8, y - 8, w + 58, h + 16, PAPER, 3);
        g.fill(x, y, x + w, y + h, 0xFF2A2A2A);
        // zone de l'adversaire
        int zTop = y + Math.round((1f - (state.zoneCenter() + state.zoneSize() / 2)) * h);
        int zBot = y + Math.round((1f - (state.zoneCenter() - state.zoneSize() / 2)) * h);
        g.fill(x + 2, Math.max(y, zTop), x + w - 2, Math.min(y + h, zBot), GOLD);
        // ton bras
        int my = y + Math.round((1f - state.marker()) * h);
        g.fill(x - 3, my - 2, x + w + 3, my + 2, RED);
        g.fill(x - 3, my - 3, x + w + 3, my - 2, INK);
        g.fill(x - 3, my + 2, x + w + 3, my + 3, INK);
        // barre de manche
        int px = x + w + 12;
        g.fill(px, y, px + 12, y + h, 0xFF2A2A2A);
        int ph = Math.round(state.progress() * h);
        g.fill(px + 2, y + h - ph, px + 10, y + h, GREEN);
        MangaHud.outlined(g, MangaHud.manga(title), width / 2f - font.width(title) * 0.6f, y - 26, 0xFFFFFFFF, 1.2f);
        Component help = state.result() == 1 ? Component.translatable("armwrestle.hxh.result_won")
                : state.result() == 2 ? Component.translatable("armwrestle.hxh.result_lost")
                : Component.translatable("armwrestle.hxh.help");
        g.drawCenteredString(font, help, width / 2, y + h + 14, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        mouseDown = keyDown = false;
        send();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
