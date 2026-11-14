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

import java.util.ArrayList;
import java.util.List;

/**
 * Aura autour du corps, en deux styles (choisis par Evan sur la maquette) :
 * <ul>
 *     <li><b>Vapeur</b> — aura calme : au repos, au Ten. Une lueur douce collée au corps ({@link AuraLayer}) et des
 *         volutes qui montent lentement et s'effacent.</li>
 *     <li><b>Flammes</b> (langues de feu en volume) — aura agressive : Ren (grandes flammes, cœur blanc), Ken (plus serrées), et toujours pour une
 *         aura « sauvage » (personnages hostiles ou incontrôlés). Des langues de feu cernées de noir naissent au pied,
 *         montent le long du corps et vacillent.</li>
 * </ul>
 * Ryu à 75 % ou plus vers l'attaque : le corps ne garde presque plus d'aura, elle se concentre en flammes aux poings.
 */
public final class AuraClient {
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
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.client.event.RenderLivingEvent.Post.class, AuraClient::onLivingRender);
    }

    /**
     * Gyo et yeux écarlates : deux lueurs dessinées À PLAT sur le visage, dans le repère de la tête (pivot au cou,
     * comme le modèle), donc toujours collées aux yeux quel que soit l'angle. Avant, c'étaient des carrés tournés vers
     * la caméra, à 0,27 bloc du centre de la tête : dès qu'on tournait la tête, le cube de la tête passait devant et
     * les lueurs disparaissaient. Vu de dos, le visage cache les yeux, comme il se doit.
     * Yeux écarlates (Kurta) : rouges, plus vifs, avec de petits éclairs qui crépitent autour.
     */
    private static void onLivingRender(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event) {
        LivingEntity e = event.getEntity();
        if (e.isInvisible() || !e.hasData(ModAttachments.HUNTER) || NenFxClient.firstPerson(e)) return;
        HunterData d = e.getData(ModAttachments.HUNTER);
        boolean scarlet = e.hasEffect(com.evandouard.hxh.effect.ModEffects.SCARLET_EYES);
        if (!scarlet && !d.gyo() && !e.hasEffect(com.evandouard.hxh.effect.ModEffects.GYO)) return;
        float pt = event.getPartialTick();
        float headYaw = Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot);
        float pitch = Mth.lerp(pt, e.xRotO, e.getXRot());
        Minecraft mc = Minecraft.getInstance();
        float time = (mc.level == null ? 0 : mc.level.getGameTime()) + pt;
        float pulse = 0.85f + 0.15f * Mth.sin(time * 0.5f);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(NenFxClient.GLOW));
        int color = scarlet ? 0xFF1E1E : d.auraColor() & 0xFFFFFF;
        // Repère de la tête : pivot au cou (là où le modèle la fait tourner), échelle selon la taille de l'entité
        float k = e.getBbHeight() / 1.8f;
        float eyeAboveNeck = 0.21f * k, face = 0.265f * k, spread = 0.115f * k;
        pose.pushPose();
        pose.translate(0, e.getEyeHeight() - eyeAboveNeck, 0);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-headYaw));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
        for (int s = -1; s <= 1; s += 2) {
            pose.pushPose();
            pose.translate(spread * s, eyeAboveNeck, face);
            float halo = (scarlet ? 0.16f : 0.13f) * k * pulse;
            quadLocal(glow, pose, halo, color, 1f);
            quadLocal(glow, pose, 0.05f * k, scarlet ? 0xFFB0B0 : 0xFFFFFF, 1f);
            pose.popPose();
        }
        // Éclairs après les deux lueurs : demander un autre tampon termine le précédent (« Not building! »)
        if (scarlet) {
            VertexConsumer bolts = buffers.getBuffer(RenderType.eyes(NenFxClient.WHITE));
            for (int s = -1; s <= 1; s += 2) {
                pose.pushPose();
                pose.translate(spread * s, eyeAboveNeck, face);
                sparks(bolts, pose, e.getId() * 7 + s, time, k);
                pose.popPose();
            }
        }
        pose.popPose();
    }

    /** Petits éclairs rouges autour d'un œil : 3 zigzags qui changent toutes les 2 images, à plat sur le visage. */
    private static void sparks(VertexConsumer vc, PoseStack pose, int seed, float time, float k) {
        int frame = (int) (time / 2f);
        for (int b = 0; b < 3; b++) {
            float r0 = hash(seed * 31 + frame * 7 + b);
            if (r0 < 0.25f) continue; // un éclair sur quatre s'éteint : ça crépite
            float ang = (r0 * 2f + b / 3f) * Mth.TWO_PI;
            float x = Mth.cos(ang) * 0.06f * k, y = Mth.sin(ang) * 0.06f * k;
            for (int seg = 0; seg < 3; seg++) {
                float len = (0.035f + 0.03f * hash(seed + frame * 13 + b * 5 + seg)) * k;
                float jitter = (hash(seed * 3 + frame * 17 + b * 11 + seg) - 0.5f) * 1.6f;
                float nx = x + Mth.cos(ang + jitter) * len, ny = y + Mth.sin(ang + jitter) * len;
                line(vc, pose, x, y, nx, ny, 0.007f * k, 0xFF3A2A, 0.95f);
                line(vc, pose, x, y, nx, ny, 0.003f * k, 0xFFE0D0, 1f);
                x = nx;
                y = ny;
            }
        }
    }

    /** Trait épais à plat (z = 0,001) entre deux points du repère local. */
    private static void line(VertexConsumer vc, PoseStack pose, float x0, float y0, float x1, float y1, float w, int rgb, float alpha) {
        float dx = x1 - x0, dy = y1 - y0, len = Mth.sqrt(dx * dx + dy * dy);
        if (len < 1e-5f) return;
        float px = -dy / len * w, py = dx / len * w;
        PoseStack.Pose last = pose.last();
        int a = Mth.clamp((int) (alpha * 255), 0, 255);
        float[][] c = {{x0 - px, y0 - py}, {x1 - px, y1 - py}, {x1 + px, y1 + py}, {x0 + px, y0 + py}};
        for (float[] v : c) vc.addVertex(last.pose(), v[0], v[1], 0.001f).setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a)
                .setUv(0.5f, 0.5f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(LIGHT)
                .setNormal(last, 0f, 1f, 0f);
    }

    private static void quadLocal(VertexConsumer vc, PoseStack pose, float h, int rgb, float alpha) {
        PoseStack.Pose last = pose.last();
        int a = Mth.clamp((int) (alpha * 255), 0, 255);
        float[][] c = {{-h, -h, 0, 1}, {h, -h, 1, 1}, {h, h, 1, 0}, {-h, h, 0, 0}};
        for (float[] v : c) vc.addVertex(last.pose(), v[0], v[1], 0).setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a)
                .setUv(v[2], v[3]).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(LIGHT)
                .setNormal(last, 0f, 1f, 0f);
    }

    /** null : pas d'aura visible (pas de Nen, Zetsu, In sans Gyo, invisible). */
    public static Look look(LivingEntity e) {
        if (e.isInvisible() || !e.hasData(ModAttachments.HUNTER)) return null;
        HunterData d = e.getData(ModAttachments.HUNTER);
        if (!d.nenUnlocked() || d.mode() == NenMode.ZETSU) return null;
        int viewerGyo = SensesClient.localGyoLevel();
        if (d.mode() == NenMode.IN && viewerGyo < com.evandouard.hxh.nen.NenSenses.GYO_SEE_IN) return null;
        int color = (d.healing() ? 0x7CFF8A : d.auraColor()) & 0xFFFFFF;
        // Ko : toute l'aura dans le poing, plus rien sur le corps ; la flamme grandit pendant la charge
        if (e.level() != null && d.koUntil() > e.level().getGameTime()) {
            float charge = com.evandouard.hxh.nen.NenDefense.koCharge(d, e.level().getGameTime());
            return new Look(true, 0.5f + 0.9f * charge, color, 0f, true);
        }
        boolean fists = d.ryuAttack() >= RYU_FISTS_FROM && d.mode() != NenMode.IN;
        float body = fists ? 0.3f : 1f;
        boolean wild = d.wildAura() || com.evandouard.hxh.hatsu.HatsuKits.wildAuraByDefault(d.kit());
        if (d.mode() == NenMode.IN) {
            // In : pâle au palier 25, nette au palier 50
            float k = Mth.clamp((viewerGyo - com.evandouard.hxh.nen.NenSenses.GYO_SEE_IN)
                    / (float) (com.evandouard.hxh.nen.NenSenses.GYO_SEE_IN_CLEAR - com.evandouard.hxh.nen.NenSenses.GYO_SEE_IN), 0f, 1f);
            return new Look(false, 0.45f * (0.3f + 0.7f * k), color, body, false);
        }
        return switch (d.mode()) {
            case REN -> new Look(true, 1.6f, color, body, fists); // Ren : aura bien plus grosse et large
            case KEN -> new Look(true, 0.6f, color, body, fists);
            case TEN -> wild ? new Look(true, 0.35f, color, body, fists) : new Look(false, 0.8f, color, body, fists);
            default -> wild ? new Look(true, 0.25f, color, body, fists) : new Look(false, 0.45f, color, body, fists);
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
            HunterData hd = e.hasData(ModAttachments.HUNTER) ? e.getData(ModAttachments.HUNTER) : null;
            Look look = look(e);
            if (look == null) continue;
            float time = mc.level.getGameTime() + pt + (e.getId() * 7.31f);
            Vec3 feet = e.getPosition(pt);
            if (look.bodyVisibility() > 0.02f) {
                if (look.flames()) flames(pose, buffers, cam, e, feet, look, time);
                else vapor(pose, buffers, cam, e, feet, look, time);
            }
            if (look.fists()) fists(pose, buffers, cam, e, pt, look, time);
            // Gyo 20 : on voit où l'adversaire concentre son Ryu (le poing chargé brille)
            if (hd != null && e != mc.player && hd.ryuAttack() >= RYU_REVEAL_FROM
                    && SensesClient.localGyoLevel() >= com.evandouard.hxh.nen.NenSenses.GYO_SEE_RYU) {
                ryuReveal(pose, buffers, cam, e, pt, look.color(), hd.ryuAttack(), time);
            }
            drew = true;
        }
        // Gyo 50 : la trajectoire des tirs qui arrivent se dessine devant eux
        if (SensesClient.localGyoLevel() >= com.evandouard.hxh.nen.NenSenses.GYO_SEE_TRAJECTORY) {
            drew |= trajectories(pose, buffers, cam, mc, pt);
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
        Mesh3D.Ctx m = new Mesh3D.Ctx(pose, buffers, cam);
        for (int i = 0; i < n; i++) {
            float phase = ((time / life) + hash(i + 17)) % 1f;
            float ang = i * 2.39996f + Mth.sin(time * 0.05f + i) * 0.3f;
            float rad = w * (0.45f + 0.3f * Math.max(0f, look.strength() - 0.6f) + 0.15f * Math.min(look.strength(), 1f));
            float y = (float) (feet.y() + h * (0.05f + 0.75f * phase));
            float size = maxTongue * Mth.sin(Mth.PI * Math.min(1f, phase * 1.3f)) * (0.7f + 0.3f * hash(i));
            if (size < 0.05f) continue;
            float flick = 1f + 0.18f * Mth.sin(time * 1.3f + i * 2.1f);
            Vec3 base = new Vec3(feet.x() + Mth.cos(ang) * rad, y, feet.z() + Mth.sin(ang) * rad);
            float a = alphaBody * Math.min(1f, (1f - phase) * 2.5f) * front(cam, feet, ang);
            tongue3d(m, base, size * 0.3f, size * flick, look.color(), a, time, i, false);
        }
        // cœur blanc additif (Ren surtout)
        if (look.strength() >= 0.6f) {
            for (int i = 0; i < n; i += 2) {
                float phase = ((time / life) + hash(i + 17)) % 1f;
                float ang = i * 2.39996f + Mth.sin(time * 0.05f + i) * 0.3f;
                float rad = w * (0.45f + 0.3f * Math.max(0f, look.strength() - 0.6f) + 0.15f * Math.min(look.strength(), 1f));
                float y = (float) (feet.y() + h * (0.05f + 0.75f * phase));
                float size = maxTongue * Mth.sin(Mth.PI * Math.min(1f, phase * 1.3f)) * (0.7f + 0.3f * hash(i)) * 0.7f;
                if (size < 0.05f) continue;
                Vec3 base = new Vec3(feet.x() + Mth.cos(ang) * rad, y, feet.z() + Mth.sin(ang) * rad);
                float a = alphaBody * Math.min(1f, (1f - phase) * 2.5f) * 0.8f * front(cam, feet, ang);
                tongue3d(m, base, size * 0.16f, size, dim(mixWhite(look.color(), 0.6f), a), a, time, i, true);
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
            float k = look.bodyVisibility() < 0.02f ? look.strength() : 1f; // Ko : le poing grossit en se chargeant
            billboard(glow, pose, cam, fist, (0.55f + 0.08f * Mth.sin(time * 0.6f)) * k, dim(look.color(), 0.9f), 0.9f);
        }
        Mesh3D.Ctx m = new Mesh3D.Ctx(pose, buffers, cam);
        for (Vec3 fist : new Vec3[]{right, left}) {
            for (int i = 0; i < 4; i++) {
                float phase = ((time / 8f) + i * 0.25f) % 1f;
                float size = 0.5f * Mth.sin(Mth.PI * phase);
                if (size < 0.04f) continue;
                Vec3 base = fist.add(Mth.cos(i * 1.57f) * 0.12, -0.15 + phase * 0.35, Mth.sin(i * 1.57f) * 0.12);
                tongue3d(m, base, size * 0.3f, size, look.color(), 1f - phase * 0.6f, time, i, false);
            }
        }
    }

    /** Ryu offensif à partir duquel le Gyo (palier 20) révèle le poing chargé. */
    public static final int RYU_REVEAL_FROM = 55;

    private static void ryuReveal(PoseStack pose, MultiBufferSource buffers, Vec3 cam, LivingEntity e, float pt, int color,
                                  int ryu, float time) {
        Mesh3D.Ctx m = new Mesh3D.Ctx(pose, buffers, cam);
        Vec3 fist = NenFxClient.hand(e, pt);
        float k = (ryu - RYU_REVEAL_FROM) / (float) (100 - RYU_REVEAL_FROM);
        float r = 0.22f + 0.2f * k + 0.03f * Mth.sin(time * 0.5f);
        Mesh3D.sphere(m, Mesh3D.glow(m), fist, r, color, 0.9f, 8, 10, null, false, true);
        Mesh3D.sphere(m, Mesh3D.glow(m), fist, r * 0.45f, 0xFFFFFF, 0.8f, 6, 8, null, false, false);
        Mesh3D.torus(m, Mesh3D.glow(m), fist, e.getViewVector(pt), r * 1.4f, 0.015f, color, 0.6f, 20, 4, false);
    }

    /** Traits légers devant les tirs (flèches, tirs d'aura) qui ne viennent pas du joueur local. */
    private static boolean trajectories(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Minecraft mc, float pt) {
        Mesh3D.Ctx m = new Mesh3D.Ctx(pose, buffers, cam);
        boolean any = false;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof net.minecraft.world.entity.projectile.Projectile p) || p.getOwner() == mc.player) continue;
            if (p.onGround() || p.distanceToSqr(cam) > 48 * 48) continue;
            Vec3 v = p.getDeltaMovement();
            if (v.lengthSqr() < 0.04) continue;
            Vec3 start = p.getPosition(pt);
            List<Vec3> path = new ArrayList<>(8);
            Vec3 pos = start, vel = v;
            boolean gravity = !p.isNoGravity();
            for (int i = 0; i < 8; i++) {
                path.add(pos);
                for (int s = 0; s < 2; s++) {
                    pos = pos.add(vel);
                    if (gravity) vel = vel.scale(0.99).add(0, -0.05, 0);
                }
            }
            // trait rouge translucide (une lueur additive virerait au blanc sur le ciel)
            Mesh3D.tube(m, Mesh3D.solid(m), path, 0.03f, 0.006f, 0xE02828, 0.6f, 4, false);
            any = true;
        }
        return any;
    }

    /** Gyo : l'aura concentrée dans les yeux, deux petites lueurs qui palpitent. */
    private static void gyoEyes(PoseStack pose, MultiBufferSource buffers, Vec3 cam, LivingEntity e, float pt, int color, float time) {
        if (!e.hasData(ModAttachments.HUNTER)) return;
        Vec3 eye = e.getEyePosition(pt);
        Vec3 fwd = e.getViewVector(pt);
        Vec3 side = fwd.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : side.normalize();
        float pulse = 0.85f + 0.15f * Mth.sin(time * 0.5f);
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(NenFxClient.GLOW));
        for (int s = -1; s <= 1; s += 2) {
            Vec3 at = eye.add(fwd.scale(0.36)).add(side.scale(0.12 * s)).add(0, 0.08, 0);
            billboard(glow, pose, cam, at, 0.24f * pulse, color, 1f);
            billboard(glow, pose, cam, at, 0.1f, 0xFFFFFF, 1f);
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

    /**
     * Langue de feu en volume : tube qui monte en ondulant, ventre arrondi en bas et pointe effilée en haut.
     * Couche externe translucide à bords denses (fresnel), cœur additif plus fin.
     */
    private static void tongue3d(Mesh3D.Ctx m, Vec3 base, float halfW, float height, int rgb, float alpha, float time,
                                 int seed, boolean core) {
        if (alpha < 0.02f || height < 0.04f) return;
        float dirA = seed * 2.39996f;
        Vec3 sway = new Vec3(Mth.cos(dirA), 0, Mth.sin(dirA));
        List<Vec3> path = new ArrayList<>(6);
        Vec3 b = base.subtract(0, height * 0.08, 0);
        for (int k = 0; k <= 5; k++) {
            float t = k / 5f;
            float wob = Mth.sin(time * 0.45f + seed * 1.3f + t * 3.2f) * halfW * 1.1f * t;
            path.add(b.add(0, height * t, 0).add(sway.scale(wob)));
        }
        Mesh3D.Profile prof = t -> halfW * (t < 0.25f ? Mth.lerp(t / 0.25f, 0.5f, 1f)
                : (float) Math.pow(Math.max(0f, 1f - (t - 0.25f) / 0.75f), 0.8));
        if (core) Mesh3D.tube(m, Mesh3D.glow(m), path, prof, rgb, alpha, 5, false);
        else Mesh3D.tube(m, Mesh3D.solid(m), path, prof, rgb, alpha * 0.9f, 7, false, true);
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
