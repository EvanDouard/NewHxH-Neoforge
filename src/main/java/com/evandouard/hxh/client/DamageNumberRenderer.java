package com.evandouard.hxh.client;

import com.evandouard.hxh.combat.DamageNumbers;
import com.evandouard.hxh.network.DamageNumberPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Affiche les chiffres flottants au-dessus des entités : ils montent et s'effacent en 1,5 s.
 * Les soins rapprochés (régénération naturelle) sont regroupés pour ne pas spammer.
 */
public final class DamageNumberRenderer {
    private static final int LIFETIME = 30;
    private static final int MERGE_WINDOW = 10;

    private static final class Num {
        float amount;
        final boolean heal;
        long start;
        final float offsetX;
        /** Dégâts du Nen (bleu, à côté des rouges) et encaissés par la défense (gris, petit). */
        float nen, blocked;

        Num(float amount, boolean heal, long start, float offsetX) {
            this.amount = amount;
            this.heal = heal;
            this.start = start;
            this.offsetX = offsetX;
        }
    }

    private static final Map<Integer, List<Num>> NUMBERS = new HashMap<>();

    private DamageNumberRenderer() {}

    public static void register() {
        DamageNumberPayload.clientHandler = DamageNumberRenderer::add;
        NeoForge.EVENT_BUS.addListener(RenderLivingEvent.Post.class, DamageNumberRenderer::onRender);
    }

    private static void add(DamageNumberPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        List<Num> list = NUMBERS.computeIfAbsent(p.entityId(), k -> new ArrayList<>());
        list.removeIf(n -> now - n.start > LIFETIME);
        if (!list.isEmpty()) {
            Num last = list.get(list.size() - 1);
            if (last.heal == p.heal() && now - last.start <= MERGE_WINDOW && p.heal()) {
                last.amount += p.amount();
                last.start = now;
                return;
            }
        }
        Num n = new Num(p.amount(), p.heal(), now, (mc.level.random.nextFloat() - 0.5f) * 0.6f);
        n.nen = p.nen();
        n.blocked = p.blocked();
        list.add(n);
    }

    private static void onRender(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        List<Num> list = NUMBERS.get(entity.getId());
        if (list == null || list.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) return;
        float time = mc.level.getGameTime() + event.getPartialTick();
        Font font = mc.font;
        PoseStack pose = event.getPoseStack();
        for (Iterator<Num> it = list.iterator(); it.hasNext(); ) {
            Num n = it.next();
            float age = time - n.start;
            if (age > LIFETIME) {
                it.remove();
                continue;
            }
            float fade = age > LIFETIME - 10 ? (LIFETIME - age) / 10f : 1f;
            int alpha = Math.max(16, (int) (255 * fade));
            pose.pushPose();
            pose.translate(n.offsetX, entity.getBbHeight() + 0.9 + age * 0.03, 0);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            float total = n.amount + n.nen;
            float scale = 0.03f * (total >= 10 && !n.heal ? 1.3f : 1f);
            pose.scale(scale, -scale, scale);
            Matrix4f matrix = pose.last().pose();
            // rouge = Force, bleu = Nen, côte à côte ; vert = soin ; gris = encaissé par la défense
            java.util.List<Component> parts = new java.util.ArrayList<>();
            java.util.List<Integer> colors = new java.util.ArrayList<>();
            if (n.heal) {
                parts.add(Component.literal(DamageNumbers.format(n.amount, true)));
                colors.add(0x55FF55);
            } else {
                if (n.amount >= 0.05f) {
                    parts.add(Component.literal(DamageNumbers.format(n.amount, false)));
                    colors.add(0xFF4040);
                }
                if (n.nen >= 0.05f) {
                    parts.add(Component.literal(DamageNumbers.format(n.nen, false)));
                    colors.add(0x4FA8FF);
                }
            }
            int gap = 4, width = -gap;
            for (Component c : parts) width += font.width(c) + gap;
            float x = -width / 2f;
            for (int i = 0; i < parts.size(); i++) {
                font.drawInBatch(parts.get(i), x, 0, (alpha << 24) | colors.get(i), true, matrix, event.getMultiBufferSource(),
                        Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
                x += font.width(parts.get(i)) + gap;
            }
            if (!n.heal && n.blocked >= 0.5f) {
                Component b = Component.translatable("hud.hxh.damage_blocked", DamageNumbers.format(n.blocked, false).substring(1));
                pose.pushPose();
                pose.scale(0.7f, 0.7f, 0.7f);
                font.drawInBatch(b, -font.width(b) / 2f, parts.isEmpty() ? 0 : 13, (alpha << 24) | 0xB0B0B0, true,
                        pose.last().pose(), event.getMultiBufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
                pose.popPose();
            }
            pose.popPose();
        }
    }
}
