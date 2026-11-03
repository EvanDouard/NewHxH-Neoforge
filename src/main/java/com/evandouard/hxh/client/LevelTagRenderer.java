package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

/**
 * Affiche « Niv. X » au-dessus des entités qui ont des données Hunter (mobs à Nen, autres joueurs).
 * Le niveau est la moyenne des stats ; la couleur dit si l'entité est plus faible ou plus forte que toi.
 * Visible seulement quand le joueur a le Gyo actif.
 */
public final class LevelTagRenderer {
    private static final double MAX_DISTANCE = 32;

    private LevelTagRenderer() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RenderLivingEvent.Post.class, LevelTagRenderer::onRender);
    }

    /** Vert : bien plus faible · jaune : à ta portée · orange : plus fort · rouge : dangereux. */
    public static int colorFor(int theirLevel, int myLevel) {
        int diff = theirLevel - myLevel;
        if (diff <= -5) return 0xFF55FF55;
        if (diff <= 5) return 0xFFFFFF55;
        if (diff <= 15) return 0xFFFFAA00;
        return 0xFFFF5555;
    }

    private static void onRender(RenderLivingEvent.Post<?, ?> event) {
        Minecraft mc = Minecraft.getInstance();
        LivingEntity entity = event.getEntity();
        if (mc.player == null || entity == mc.player || entity.isInvisible() || mc.options.hideGui) return;
        if (!SensesClient.localGyo()) return; // le niveau d'un adversaire se lit avec le Gyo
        if (!entity.hasData(ModAttachments.HUNTER)) return;
        HunterData data = entity.getData(ModAttachments.HUNTER);
        if (!(entity instanceof Player) && !data.nenUnlocked() && data.hunterLevel() == 0) return;
        if (entity.distanceToSqr(mc.player) > MAX_DISTANCE * MAX_DISTANCE) return;

        int level = data.hunterLevel();
        int mine = mc.player.getData(ModAttachments.HUNTER).hunterLevel();
        Component text = Component.translatable("hud.hxh.level_tag", level);

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // Au-dessus de la tête, et encore au-dessus si un nom est déjà affiché
        double y = entity.getBbHeight() + 0.5 + (entity.shouldShowName() ? 0.3 : 0);
        pose.translate(0, y, 0);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.025f, -0.025f, 0.025f);
        Matrix4f matrix = pose.last().pose();
        Font font = mc.font;
        float x = -font.width(text) / 2f;
        int background = (int) (mc.options.getBackgroundOpacity(0.25f) * 255f) << 24;
        font.drawInBatch(text, x, 0, colorFor(level, mine), false, matrix, event.getMultiBufferSource(),
                Font.DisplayMode.SEE_THROUGH, background, event.getPackedLight());
        font.drawInBatch(text, x, 0, colorFor(level, mine), false, matrix, event.getMultiBufferSource(),
                Font.DisplayMode.NORMAL, 0, event.getPackedLight());
        pose.popPose();
    }
}
