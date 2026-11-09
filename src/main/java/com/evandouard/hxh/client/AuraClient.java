package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Aura autour du corps, en deux styles (choisis par Evan sur la maquette) :
 * <ul>
 *     <li><b>Vapeur</b> — aura calme : au repos, au Ten. Une lueur douce collée au corps ({@link AuraLayer}) et des
 *         volutes qui montent lentement et s'effacent.</li>
 *     <li><b>Flammes</b> — aura agressive : Ren (grandes flammes, cœur blanc), Ken (plus serrées), et toujours pour une
 *         aura « sauvage » (personnages hostiles ou incontrôlés). Des langues de feu cernées de noir naissent au pied,
 *         montent le long du corps et vacillent.</li>
 * </ul>
 * Ryu à 75 % ou plus vers l'attaque : le corps ne garde presque plus d'aura, elle se concentre en flammes aux poings.
 */
public final class AuraClient {
    private static final ResourceLocation FLAME = tex("flame"), FLAME_CORE = tex("flame_core");
    private static final int LIGHT = 0xF000F0;
    public static final int RYU_FISTS_FROM = 75;

    /**
     * Aspect de l'aura d'une entité pour cette image.
     *
     * @param flames         style flammes (sinon vapeur)
     * @param strength       0..1, taille et nombre des flammes ou volutes
     * @param bodyVisibility 1 normalement, faible quand le Ryu envoie l'aura dans les poings
     * @param fists          flammes aux poings (Ryu offensif)
     */
    public record Look(boolean flames, float strength, int color, float bodyVisibility, boolean fists) {}

    private AuraClient() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath("hxh", "textures/fx/" + name + ".png");
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, AuraClient::onRender);
    }

    /** null : pas d'aura visible (pas de Nen, Zetsu, In sans Gyo, invisible). */
    public static Look look(LivingEntity e) {
        if (e.isInvisible() || !e.hasData(ModAttachments.HUNTER)) return null;
        HunterData d = e.getData(ModAttachments.HUNTER);
        if (!d.nenUnlocked() || d.mode() == NenMode.ZETSU) return null;
        if (d.mode() == NenMode.IN && !SensesClient.localGyo()) return null;
        int color = (d.healing() ? 0x7CFF8A : d.auraColor()) & 0xFFFFFF;
        boolean fists = d.ryuAttack() >= RYU_FISTS_FROM && d.mode() != NenMode.IN;
        float body = fists ? 0.3f : 1f;
        return switch (d.mode()) {
            case REN -> new Look(true, 1f, color, body, fists);
            case KEN -> new Look(true, 0.6f, color, body, fists);
            case TEN -> d.wildAura() ? new Look(true, 0.35f, color, body, fists) : new Look(false, 0.8f, color, body, fists);
            default -> d.wildAura() ? new Look(true, 0.25f, color, body, fists) : new Look(false, 0.45f, color, body, fists);
        };
    }

    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        boolean drew = false;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity e) || entity.distanceToSqr(cam) > 64 * 64) continue;
            // Son propre corps en vue à la première personne : rien (ça boucherait la vue)
            if (NenFxClient.firstPerson(e)) continue;
            Look look = look(e);
            if (look == null) continue;
            float time = mc.level.getGameTime() + pt + (e.getId() * 7.31f);
            Vec3 feet = e.getPosition(pt);
            if (look.flames()) flames(pose, buffers, cam, e, feet, look, time);
            else vapor(pose, buffers, cam, e, feet, look, time);
            if (look.fists()) fists(pose, buffers, cam, e, pt, look, time);
            drew = true;
        }
        if (drew) buffers.endBatch();
    }

    /** Langues de feu : naissent au pied, montent le long du corps en grandissant puis s'effilent au sommet. */
    private static void flames(PoseStack pose, MultiBufferSource buffers, Vec3 cam, LivingEntity e, Vec3 feet,
                               Look look, float time) {
        float h = e.getBbHeight(), w = e.getBbWidth();
        int n = Math.round(8 + 22 * look.strength());
        float life = 14f - 4f * look.strength();
        float maxTongue = h * (0.25f + 0.55f * look.strength());
        float alphaBody = look.fists() ? 0.1f : 1f;
        VertexConsumer outer = buffers.getBuffer(RenderType.entityTranslucent(FLAME));
        for (int i = 0; i < n; i++) {
            float phase = ((time / life) + hash(i + 17)) % 1f;
            float ang = i * 2.39996f + Mth.sin(time * 0.05f + i) * 0.3f;
            float rad = w * (0.45f + 0.15f * look.strength());
            float y = (float) (feet.y() + h * (0.05f + 0.75f * phase));
            float size = maxTongue * Mth.sin(Mth.PI * Math.min(1f, phase * 1.3f)) * (0.7f + 0.3f * hash(i));
            if (size < 0.05f) continue;
            float flick = 1f + 0.18f * Mth.sin(time * 1.3f + i * 2.1f);
            Vec3 base = new Vec3(feet.x() + Mth.cos(ang) * rad, y, feet.z() + Mth.sin(ang) * rad);
            float a = alphaBody * Math.min(1f, (1f - phase) * 2.5f) * front(cam, feet, ang);
            tongue(outer, pose, cam, base, size * 0.45f, size * flick, look.color(), a);
        }
        // cœur blanc additif (Ren surtout)
        if (look.strength() >= 0.6f) {
            VertexConsumer core = buffers.getBuffer(RenderType.eyes(FLAME_CORE));
            for (int i = 0; i < n; i += 2) {
                float phase = ((time / life) + hash(i + 17)) % 1f;
                float ang = i * 2.39996f + Mth.sin(time * 0.05f + i) * 0.3f;
                float rad = w * (0.45f + 0.15f * look.strength());
                float y = (float) (feet.y() + h * (0.05f + 0.75f * phase));
                float size = maxTongue * Mth.sin(Mth.PI * Math.min(1f, phase * 1.3f)) * (0.7f + 0.3f * hash(i)) * 0.7f;
                if (size < 0.05f) continue;
                Vec3 base = new Vec3(feet.x() + Mth.cos(ang) * rad, y, feet.z() + Mth.sin(ang) * rad);
                float a = alphaBody * Math.min(1f, (1f - phase) * 2.5f) * 0.8f * front(cam, feet, ang);
                tongue(core, pose, cam, base, size * 0.45f, size, dim(mixWhite(look.color(), 0.6f), a), a);
            }
        }
    }

    /** Volutes : petites lueurs qui montent lentement autour du corps, dérivent et s'effacent. */
    private static void vapor(PoseStack pose, MultiBufferSource buffers, Vec3 cam, LivingEntity e, Vec3 feet,
                              Look look, float time) {
        float h = e.getBbHeight(), w = e.getBbWidth();
        int n = Math.round(4 + 10 * look.strength());
        float life = 40f;
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(NenFxClient.GLOW));
        float body = look.bodyVisibility();
        for (int i = 0; i < n; i++) {
            float phase = ((time / life) + hash(i + 17)) % 1f;
            float ang = i * 2.39996f;
            float rad = w * 0.55f + phase * 0.25f;
            float drift = Mth.sin(time * 0.04f + i * 1.7f) * 0.25f;
            float y = (float) (feet.y() + h * (0.15f + 0.6f * hash(i)) + phase * h * 0.7f);
            Vec3 at = new Vec3(feet.x() + Mth.cos(ang) * rad + drift, y, feet.z() + Mth.sin(ang) * rad - drift);
            float size = (0.3f + 0.45f * phase) * (0.6f + 0.6f * look.strength());
            float a = Mth.sin(Mth.PI * phase) * 0.9f * body;
            billboard(vc, pose, cam, at, size, dim(mixWhite(look.color(), 0.35f), a), a);
        }
    }

    /** Ryu offensif : l'aura brûle autour des deux poings. */
    private static void fists(PoseStack pose, MultiBufferSource buffers, Vec3 cam, LivingEntity e, float pt, Look look,
                              float time) {
        Vec3 right = NenFxClient.hand(e, pt);
        Vec3 center = e.getPosition(pt).add(0, right.y - e.getPosition(pt).y, 0);
        Vec3 left = center.add(center.subtract(right).multiply(1, 0, 1));
        // Une couche à la fois : demander un autre tampon termine le précédent
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(NenFxClient.GLOW));
        for (Vec3 fist : new Vec3[]{right, left}) {
            billboard(glow, pose, cam, fist, 0.55f + 0.08f * Mth.sin(time * 0.6f), dim(look.color(), 0.9f), 0.9f);
        }
        VertexConsumer outer = buffers.getBuffer(RenderType.entityTranslucent(FLAME));
        for (Vec3 fist : new Vec3[]{right, left}) {
            for (int i = 0; i < 4; i++) {
                float phase = ((time / 8f) + i * 0.25f) % 1f;
                float size = 0.5f * Mth.sin(Mth.PI * phase);
                Vec3 base = fist.add(Mth.cos(i * 1.57f) * 0.12, -0.15 + phase * 0.35, Mth.sin(i * 1.57f) * 0.12);
                tongue(outer, pose, cam, base, size * 0.5f, size, look.color(), 1f - phase * 0.6f);
            }
        }
    }

    /** Les flammes devant le corps (face à la caméra) sont presque effacées : on voit le personnage, pas un mur de feu. */
    private static float front(Vec3 cam, Vec3 feet, float ang) {
        Vec3 to = new Vec3(cam.x - feet.x, 0, cam.z - feet.z);
        if (to.lengthSqr() < 1e-4) return 1f;
        to = to.normalize();
        double facing = Mth.cos(ang) * to.x + Mth.sin(ang) * to.z; // 1 = pile devant
        return facing > 0.35 ? 0.15f : 1f;
    }

    private static float hash(int i) {
        int x = i * 0x45d9f3b;
        x = ((x >>> 16) ^ x) * 0x45d9f3b;
        return ((x >>> 8) & 0xFFFF) / 65535f;
    }

    /** Couleur assombrie : les couches additives ne tiennent pas compte de l'alpha. */
    private static int dim(int rgb, float a) {
        a = Mth.clamp(a, 0f, 1f);
        return ((int) (((rgb >> 16) & 255) * a) << 16) | ((int) (((rgb >> 8) & 255) * a) << 8) | (int) ((rgb & 255) * a);
    }

    private static int mixWhite(int rgb, float t) {
        int r = (int) Mth.lerp(t, (rgb >> 16) & 255, 255);
        int g = (int) Mth.lerp(t, (rgb >> 8) & 255, 255);
        int b = (int) Mth.lerp(t, rgb & 255, 255);
        return (r << 16) | (g << 8) | b;
    }

    /** Quad vertical tourné vers la caméra autour de l'axe Y, pied au point donné (flammes). */
    private static void tongue(VertexConsumer vc, PoseStack pose, Vec3 cam, Vec3 base, float halfW, float height,
                               int rgb, float alpha) {
        Vec3 to = cam.subtract(base);
        Vec3 side = new Vec3(-to.z, 0, to.x);
        side = side.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : side.normalize().scale(halfW);
        Vec3 up = new Vec3(0, height, 0);
        Vec3 b = base.subtract(0, height * 0.08, 0);
        vertex(vc, pose, cam, b.subtract(side), rgb, alpha, 0, 1);
        vertex(vc, pose, cam, b.add(side), rgb, alpha, 1, 1);
        vertex(vc, pose, cam, b.add(side).add(up), rgb, alpha, 1, 0);
        vertex(vc, pose, cam, b.subtract(side).add(up), rgb, alpha, 0, 0);
    }

    /** Quad face à la caméra centré sur le point. */
    private static void billboard(VertexConsumer vc, PoseStack pose, Vec3 cam, Vec3 at, float half, int rgb, float alpha) {
        Vec3 to = cam.subtract(at).normalize();
        Vec3 right = to.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(to).normalize();
        Vec3 r = right.scale(half), u = up.scale(half);
        vertex(vc, pose, cam, at.subtract(r).subtract(u), rgb, alpha, 0, 1);
        vertex(vc, pose, cam, at.add(r).subtract(u), rgb, alpha, 1, 1);
        vertex(vc, pose, cam, at.add(r).add(u), rgb, alpha, 1, 0);
        vertex(vc, pose, cam, at.subtract(r).add(u), rgb, alpha, 0, 0);
    }

    private static void vertex(VertexConsumer vc, PoseStack pose, Vec3 cam, Vec3 p, int rgb, float alpha, float u, float v) {
        PoseStack.Pose last = pose.last();
        vc.addVertex(last.pose(), (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, Mth.clamp((int) (alpha * 255), 0, 255))
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LIGHT)
                .setNormal(last, 0f, 1f, 0f);
    }
}
