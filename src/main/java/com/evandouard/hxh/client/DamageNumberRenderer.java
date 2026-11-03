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
        list.add(new Num(p.amount(), p.heal(), now, (mc.level.random.nextFloat() - 0.5f) * 0.6f));
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
            int rgb = n.heal ? 0x55FF55 : 0xFF5555;
            Component text = Component.literal(DamageNumbers.format(n.amount, n.heal));
            pose.pushPose();
            pose.translate(n.offsetX, entity.getBbHeight() + 0.9 + age * 0.03, 0);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            float scale = 0.03f * (n.amount >= 10 && !n.heal ? 1.3f : 1f);
            pose.scale(scale, -scale, scale);
            Matrix4f matrix = pose.last().pose();
            float x = -font.width(text) / 2f;
            font.drawInBatch(text, x, 0, (alpha << 24) | rgb, true, matrix, event.getMultiBufferSource(),
                    Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            pose.popPose();
        }
    }
}
