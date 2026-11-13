package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.nen.EnShape;
import com.evandouard.hxh.nen.NenSenses;
import com.evandouard.hxh.network.EnShapePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Forme de l'En (carnet, maîtrise 50) : sphère, demi-sphère, cube ou cylindre. Le volume reste celui de la sphère de
 * base : on règle le rayon (et, pour le cylindre, la hauteur au-dessus), le reste est calculé et affiché. Une forme
 * impossible est refusée ici et par le serveur.
 */
public class EnShapeScreen extends Screen {
    private static final float STEP = 0.5f;
    @Nullable
    private final Screen parent;
    private EnShape shape;
    private float a, b;
    private Button apply;

    public EnShapeScreen(@Nullable Screen parent) {
        super(Component.translatable("screen.hxh.en_shape.title"));
        this.parent = parent;
        HunterData d = data();
        shape = EnShape.byId(d.enShape());
        a = d.enA();
        b = d.enB();
    }

    private static HunterData data() {
        return Minecraft.getInstance().player.getData(ModAttachments.HUNTER);
    }

    private double baseRadius() {
        return NenSenses.enRadius(data());
    }

    @Override
    protected void init() {
        int cx = width / 2, y = 52;
        addRenderableWidget(Button.builder(Component.literal("◀"), btn -> {
            shape = EnShape.values()[(shape.ordinal() + EnShape.values().length - 1) % EnShape.values().length];
            rebuildWidgets();
        }).bounds(cx - 100, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(shape.translationKey()), btn -> {
            shape = shape.next();
            rebuildWidgets();
        }).bounds(cx - 76, y, 152, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), btn -> {
            shape = shape.next();
            rebuildWidgets();
        }).bounds(cx + 80, y, 20, 20).build());
        if (shape.adjustable()) {
            stepper(cx, y + 34, () -> a = Math.max(0, a - STEP), () -> a = Math.min((float) EnShape.MAX_SIZE, a + STEP));
        }
        if (shape.twoParams()) {
            stepper(cx, y + 58, () -> b = Math.max(0, b - STEP), () -> b = Math.min((float) EnShape.MAX_SIZE, b + STEP));
        }
        apply = addRenderableWidget(Button.builder(Component.translatable("screen.hxh.en_shape.apply"), btn -> {
            PacketDistributor.sendToServer(new EnShapePayload(shape.id(), a, b));
            onClose();
        }).bounds(cx - 102, height - 32, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), btn -> onClose())
                .bounds(cx + 2, height - 32, 100, 20).build());
        apply.active = refusal() == null;
    }

    /** Boutons − et + de part et d'autre d'une valeur. */
    private void stepper(int cx, int y, Runnable minus, Runnable plus) {
        addRenderableWidget(Button.builder(Component.literal("−"), btn -> {
            minus.run();
            apply.active = refusal() == null;
        }).bounds(cx - 100, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            plus.run();
            apply.active = refusal() == null;
        }).bounds(cx + 80, y, 20, 20).build());
    }

    @Nullable
    private String refusal() {
        return EnShape.refusal(shape, baseRadius(), a, b);
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = width / 2;
        g.drawCenteredString(font, title, cx, 16, 0xFFFFFF);
        double r = baseRadius();
        g.drawCenteredString(font, Component.translatable("screen.hxh.en_shape.volume", f(r),
                Math.round(EnShape.volume(r))).withStyle(ChatFormatting.GRAY), cx, 32, 0xFFFFFF);
        if (shape.adjustable()) {
            g.drawCenteredString(font, Component.translatable(shape == EnShape.CUBE ? "screen.hxh.en_shape.half_side"
                    : "screen.hxh.en_shape.radius", f(a)), cx, 92, 0xFFFFFF);
        }
        if (shape.twoParams()) g.drawCenteredString(font, Component.translatable("screen.hxh.en_shape.up", f(b)), cx, 116, 0xFFFFFF);
        // Ce qui est calculé pour garder le volume
        String refusal = refusal();
        int y = shape.twoParams() ? 144 : shape.adjustable() ? 120 : 92;
        if (refusal != null) {
            g.drawCenteredString(font, Component.translatable(refusal).withStyle(ChatFormatting.RED), cx, y, 0xFFFFFF);
            return;
        }
        EnShape.Dims d = EnShape.resolve(shape, r, a, b);
        Component line = switch (shape) {
            case SPHERE -> Component.translatable("screen.hxh.en_shape.sphere_dims", f(d.radius()));
            case HALF_SPHERE -> Component.translatable("screen.hxh.en_shape.half_sphere_dims", f(d.radius()));
            case CUBE -> Component.translatable("screen.hxh.en_shape.cube_dims", f(d.radius() * 2), f(d.up()));
            case CYLINDER -> Component.translatable("screen.hxh.en_shape.cylinder_dims", f(d.down()), f(d.up() + d.down()));
        };
        g.drawCenteredString(font, line.copy().withStyle(ChatFormatting.AQUA), cx, y, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.hxh.en_shape.origin").withStyle(ChatFormatting.DARK_GRAY),
                cx, y + 14, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
