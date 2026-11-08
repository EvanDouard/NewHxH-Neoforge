package com.evandouard.hxh.client;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.fx.NenFx;
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
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Dessine les effets du Nen ({@link NenFx}) avec des formes dessinées à la main (contour noir, aplats teintés),
 * sans particules de Minecraft : étoiles d'impact, chaînes maillon par maillon, éclairs, anneaux, aiguilles…
 */
public final class NenFxClient {
    private static ResourceLocation tex(String name) {
        return HxHMod.id("textures/fx/" + name + ".png");
    }

    static final ResourceLocation IMPACT = tex("impact"), SLASH = tex("slash"), RING = tex("ring"), ORB = tex("orb"),
            GLOW = tex("glow"), CHAIN = tex("chain_link"), CROSS = tex("cross"), CLAW = tex("claw"),
            NEEDLE = tex("needle"), WHITE = tex("white");
    private static final int LIGHT = 0xF000F0;
    private static final int INK = 0xFF111111;

    /** Un effet en cours. */
    static final class Fx {
        final NenFx.Kind kind;
        final int source, target;
        final Vec3 at;
        final float size;
        final int color, duration;
        final long start;
        final int seed;
        /** Aiguille : décalage du point d'impact sur la cible et direction de vol figés à l'impact. */
        Vec3 offset, dir;
        boolean impactDone;

        Fx(NenFx.Payload p, long start) {
            this.kind = p.kindEnum();
            this.source = p.source();
            this.target = p.target();
            this.at = new Vec3(p.x(), p.y(), p.z());
            this.size = p.size();
            this.color = p.color();
            this.duration = Math.max(1, p.duration());
            this.start = start;
            this.seed = (int) (start * 31 + p.source() * 17 + p.target() * 7 + p.kind());
        }
    }

    private static final List<Fx> ACTIVE = new ArrayList<>();
    private static final List<Fx> PENDING = new ArrayList<>();

    private NenFxClient() {}

    public static void register() {
        NenFx.clientHandler = NenFxClient::add;
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, NenFxClient::onRender);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> tick());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> ACTIVE.clear());
        ImpactFrameClient.register();
    }

    private static void add(NenFx.Payload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (p.kindEnum() == NenFx.Kind.CLEAR) {
            ACTIVE.removeIf(f -> f.source == p.source() && f.kind == NenFx.Kind.CHARGE_ORB);
            return;
        }
        if (p.kindEnum() == NenFx.Kind.IMPACT_FRAME) {
            ImpactFrameClient.trigger(new Vec3(p.x(), p.y(), p.z()), p.duration());
            return;
        }
        ACTIVE.add(new Fx(p, mc.level.getGameTime()));
    }

    /** Nombre d'effets en cours (client scripté de la CI). */
    public static int activeCount() {
        return ACTIVE.size();
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE.clear();
            return;
        }
        long now = mc.level.getGameTime();
        ACTIVE.removeIf(f -> now - f.start > f.duration);
        ACTIVE.addAll(PENDING);
        PENDING.clear();
    }

    // =====================================================================
    // Rendu
    // =====================================================================

    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        Ctx c = new Ctx(event.getPoseStack(), mc.renderBuffers().bufferSource(), camera, pt, mc.level.getGameTime());
        Iterator<Fx> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Fx f = it.next();
            float age = c.now - f.start + pt;
            if (age > f.duration) continue;
            try {
                draw(c, f, age);
            } catch (RuntimeException ex) {
                HxHMod.LOGGER.warn("[HxH] Effet {} ignoré", f.kind, ex);
                it.remove();
            }
        }
        c.buffers.endBatch();
    }

    /** Contexte de dessin d'une image. */
    static final class Ctx {
        final PoseStack pose;
        final MultiBufferSource.BufferSource buffers;
        final Camera camera;
        final Vec3 cam;
        final float pt;
        final long now;

        Ctx(PoseStack pose, MultiBufferSource.BufferSource buffers, Camera camera, float pt, long now) {
            this.pose = pose;
            this.buffers = buffers;
            this.camera = camera;
            this.cam = camera.getPosition();
            this.pt = pt;
            this.now = now;
        }
    }

    private static void draw(Ctx c, Fx f, float age) {
        float t = age / f.duration;
        switch (f.kind) {
            case IMPACT -> impact(c, f.at, f.size, f.color, t, f.seed, IMPACT);
            case SLASH -> impact(c, f.at, f.size, f.color, t, f.seed, SLASH);
            case SHOCKWAVE -> shockwave(c, f, t);
            case ORB -> orb(c, f, age);
            case BOLT -> bolt(c, f, age);
            case CRACKLE -> crackle(c, f, age);
            case CHAIN -> chain(c, f, age);
            case HOLY_CHAIN -> holyChain(c, f, age);
            case NEEDLE -> needle(c, f, age);
            case FLARE -> flare(c, f, t);
            case CHARGE_ORB -> chargeOrb(c, f, age);
            case QUAKE -> quake(c, f, age);
            case CLEAR, IMPACT_FRAME -> {}
        }
    }

    // ---------- Positions ----------

    private static Entity entity(int id) {
        Minecraft mc = Minecraft.getInstance();
        return id < 0 || mc.level == null ? null : mc.level.getEntity(id);
    }

    private static Vec3 center(Entity e, float pt) {
        return e.getPosition(pt).add(0, e.getBbHeight() * 0.5, 0);
    }

    /** L'entité est celle d'où l'on regarde, en vue subjective. */
    static boolean firstPerson(Entity e) {
        Minecraft mc = Minecraft.getInstance();
        return e == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    }

    /** Main droite (approximative) d'une entité : d'où partent chaînes, boules et aiguilles. */
    static Vec3 hand(Entity e, float pt) {
        if (firstPerson(e)) {
            // Vue subjective : là où l'on voit sa main à l'écran (devant, en bas à droite)
            Vec3 look = e.getViewVector(pt);
            Vec3 right = look.cross(new Vec3(0, 1, 0));
            right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
            Vec3 up = right.cross(look).normalize();
            return e.getEyePosition(pt).add(look.scale(0.9)).add(right.scale(0.35)).add(up.scale(-0.3));
        }
        float yaw = e instanceof LivingEntity l ? Mth.rotLerp(pt, l.yBodyRotO, l.yBodyRot) : e.getViewYRot(pt);
        double r = yaw * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Math.sin(r), 0, Math.cos(r));
        Vec3 right = new Vec3(-Math.cos(r), 0, -Math.sin(r));
        double w = Math.max(0.6, e.getBbWidth());
        return e.getPosition(pt).add(0, e.getBbHeight() * 0.55, 0).add(right.scale(0.35 * w / 0.6)).add(forward.scale(0.3));
    }

    // ---------- Primitives ----------

    private static int a(int argb) { return argb >>> 24; }
    private static int r(int argb) { return (argb >> 16) & 255; }
    private static int g(int argb) { return (argb >> 8) & 255; }
    private static int b(int argb) { return argb & 255; }

    private static void vertex(VertexConsumer vc, Ctx c, Vec3 p, int color, float alpha, float u, float v) {
        PoseStack.Pose pose = c.pose.last();
        vc.addVertex(pose.pose(), (float) (p.x - c.cam.x), (float) (p.y - c.cam.y), (float) (p.z - c.cam.z))
                .setColor(r(color), g(color), b(color), Mth.clamp((int) (a(color) * alpha), 0, 255))
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LIGHT)
                .setNormal(pose, 0f, 1f, 0f);
    }

    /** Quadrilatère p0 p1 p2 p3 (dans l'ordre), texture entière. */
    private static void quad(Ctx c, RenderType type, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int color, float alpha) {
        VertexConsumer vc = c.buffers.getBuffer(type);
        vertex(vc, c, p0, color, alpha, 0, 1);
        vertex(vc, c, p1, color, alpha, 1, 1);
        vertex(vc, c, p2, color, alpha, 1, 0);
        vertex(vc, c, p3, color, alpha, 0, 0);
    }

    /** Image face à la caméra, tournée de {@code angle}. */
    private static void billboard(Ctx c, RenderType type, Vec3 at, float half, float angle, int color, float alpha) {
        Quaternionf q = new Quaternionf(c.camera.rotation()).rotateZ(angle);
        Vec3[] p = new Vec3[4];
        float[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (int i = 0; i < 4; i++) {
            Vector3f v = new Vector3f(corners[i][0] * half, corners[i][1] * half, 0).rotate(q);
            p[i] = at.add(v.x, v.y, v.z);
        }
        quad(c, type, p[0], p[1], p[2], p[3], color, alpha);
    }

    /** Ruban face à la caméra de a à b (éclairs). */
    private static void ribbon(Ctx c, RenderType type, Vec3 a, Vec3 b, float width, int color, float alpha) {
        Vec3 d = b.subtract(a);
        Vec3 view = a.add(b).scale(0.5).subtract(c.cam);
        Vec3 side = d.cross(view);
        if (side.lengthSqr() < 1e-8) return;
        side = side.normalize().scale(width / 2);
        quad(c, type, a.subtract(side), b.subtract(side), b.add(side), a.add(side), color, alpha);
    }

    /** Maillon de chaîne centré en {@code at}, allongé selon {@code dir}, dans le plan (dir, n). */
    private static void link(Ctx c, Vec3 at, Vec3 dir, Vec3 n, float length, float width, int color, float alpha) {
        Vec3 h = dir.scale(length / 2);
        Vec3 w = n.scale(width / 2);
        quad(c, RenderType.entityTranslucent(CHAIN), at.subtract(h).subtract(w), at.add(h).subtract(w),
                at.add(h).add(w), at.subtract(h).add(w), color, alpha);
    }

    /** Deux vecteurs perpendiculaires à {@code dir}. */
    private static Vec3[] normals(Vec3 dir) {
        Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 n1 = dir.cross(up).normalize();
        Vec3 n2 = dir.cross(n1).normalize();
        return new Vec3[]{n1, n2};
    }

    static float hash(int seed, int i) {
        int h = seed * 0x9E3779B1 + i * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    // ---------- Effets ----------

    /** Étoile d'impact (ou croissant) : pop rapide puis s'efface, avec un halo coloré. */
    private static void impact(Ctx c, Vec3 at, float size, int color, float t, int seed, ResourceLocation texture) {
        float s = t < 0.25f ? Mth.lerp(t / 0.25f, 0.3f, 1.15f) : Mth.lerp((t - 0.25f) / 0.75f, 1.15f, 1.0f);
        float alpha = t < 0.6f ? 1f : 1f - (t - 0.6f) / 0.4f;
        float angle = hash(seed, 1) * Mth.TWO_PI;
        float half = 0.5f * size * s;
        billboard(c, RenderType.eyes(GLOW), at, half * 1.5f, 0, color, alpha * 0.7f);
        billboard(c, RenderType.entityTranslucent(texture), at, half, angle, color, alpha);
    }

    /** Anneau au sol qui s'élargit. */
    private static void shockwave(Ctx c, Fx f, float t) {
        Entity src = entity(f.source);
        Vec3 base = (src != null ? src.getPosition(c.pt) : f.at).add(0, 0.08, 0);
        float radius = Math.max(0.2f, f.size * ease(t * 1.2f));
        float alpha = 1f - t;
        quad(c, RenderType.entityTranslucent(RING), base.add(-radius, 0, -radius), base.add(radius, 0, -radius),
                base.add(radius, 0, radius), base.add(-radius, 0, radius), f.color, alpha);
        float inner = radius * 0.6f;
        quad(c, RenderType.eyes(GLOW), base.add(-inner, 0.02, -inner), base.add(inner, 0.02, -inner),
                base.add(inner, 0.02, inner), base.add(-inner, 0.02, inner), f.color, alpha * 0.5f);
    }

    /** Boule d'aura avec traînée ; à l'arrivée, impact. */
    private static void orb(Ctx c, Fx f, float age) {
        Entity src = entity(f.source);
        Entity tgt = entity(f.target);
        Vec3 from = src != null ? hand(src, c.pt) : f.at;
        Vec3 to = tgt != null ? center(tgt, c.pt) : f.at;
        float travel = (float) Math.max(3, from.distanceTo(to) / 1.6);
        float p = Math.min(1f, age / travel);
        if (p >= 1f) {
            if (!f.impactDone) {
                f.impactDone = true;
                PENDING.add(localFx(NenFx.Kind.IMPACT, to, f.size * 1.6f, f.color, 8));
            }
            return;
        }
        float half = 0.25f * f.size;
        for (int k = 5; k >= 1; k--) {
            float q = p - k * 0.05f;
            if (q <= 0) continue;
            billboard(c, RenderType.eyes(GLOW), from.lerp(to, q), half * (1f - k * 0.13f) * 1.4f, 0, f.color, 0.55f - k * 0.09f);
        }
        Vec3 pos = from.lerp(to, p);
        billboard(c, RenderType.eyes(GLOW), pos, half * 2.2f, 0, f.color, 0.8f);
        billboard(c, RenderType.entityTranslucent(ORB), pos, half, age * 0.3f, f.color, 1f);
    }

    /** Effet ajouté côté client (impact d'arrivée d'une boule…). */
    private static Fx localFx(NenFx.Kind kind, Vec3 at, float size, int color, int duration) {
        Minecraft mc = Minecraft.getInstance();
        return new Fx(new NenFx.Payload(kind.ordinal(), -1, -1, at.x, at.y, at.z, size, color, duration),
                mc.level.getGameTime());
    }

    /** Éclair dentelé : la forme change toutes les deux images, cœur blanc et halo coloré. */
    private static void bolt(Ctx c, Fx f, float age) {
        Entity src = entity(f.source);
        Entity tgt = entity(f.target);
        Vec3 from = src != null ? hand(src, c.pt) : f.at;
        Vec3 to = tgt != null ? center(tgt, c.pt) : f.at;
        float alpha = age > f.duration - 3 ? (f.duration - age) / 3f : 1f;
        zigzag(c, from, to, 12, 0.35f, f.seed + (int) (age / 2), 0.07f, f.color, alpha);
        if (!f.impactDone && age > 1) {
            f.impactDone = true;
            PENDING.add(localFx(NenFx.Kind.IMPACT, to, 1.2f, f.color, 8));
        }
    }

    private static void zigzag(Ctx c, Vec3 from, Vec3 to, int segments, float jitter, int seed, float width, int color, float alpha) {
        Vec3 d = to.subtract(from);
        if (d.lengthSqr() < 1e-6) return;
        Vec3[] n = normals(d.normalize());
        Vec3 prev = from;
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            Vec3 p = from.add(d.scale(t));
            if (i < segments) {
                float j = jitter * (float) Math.sin(Math.PI * t);
                p = p.add(n[0].scale((hash(seed, i * 2) - 0.5f) * 2 * j)).add(n[1].scale((hash(seed, i * 2 + 1) - 0.5f) * 2 * j));
            }
            ribbon(c, RenderType.eyes(WHITE), prev, p, width * 2.2f, color, alpha * 0.6f);
            ribbon(c, RenderType.entityTranslucent(WHITE), prev, p, width * 0.9f, INK, alpha);
            ribbon(c, RenderType.eyes(WHITE), prev, p, width * 0.45f, 0xFFFFFFFF, alpha);
            prev = p;
        }
    }

    /** Petits arcs électriques autour du corps, renouvelés toutes les deux images. */
    private static void crackle(Ctx c, Fx f, float age) {
        Entity src = entity(f.source);
        if (src == null || firstPerson(src)) return; // autour de soi : il passerait devant la caméra
        Vec3 base = src.getPosition(c.pt);
        int frame = (int) (age / 2);
        float w = Math.max(0.6f, src.getBbWidth()), h = src.getBbHeight();
        for (int k = 0; k < 3; k++) {
            int s = f.seed + frame * 13 + k * 101;
            Vec3 a = base.add((hash(s, 1) - 0.5) * w * 1.4, hash(s, 2) * h, (hash(s, 3) - 0.5) * w * 1.4);
            Vec3 b = a.add((hash(s, 4) - 0.5) * 0.9, (hash(s, 5) - 0.5) * 0.9, (hash(s, 6) - 0.5) * 0.9);
            zigzag(c, a, b, 4, 0.15f, s, 0.07f, f.color, 0.9f);
        }
    }

    /** Chaîne de la main du lanceur vers la cible ; une fois arrivée, elle s'enroule autour d'elle. */
    private static void chain(Ctx c, Fx f, float age) {
        Entity src = entity(f.source);
        Entity tgt = entity(f.target);
        Vec3 from = src != null ? hand(src, c.pt) : f.at;
        Vec3 to = tgt != null ? center(tgt, c.pt) : f.at;
        float fade = age > f.duration - 6 ? (f.duration - age) / 6f : 1f;
        float extend = ease(age / 4f);
        Vec3 d = to.subtract(from);
        double dist = d.length();
        if (dist < 0.05) return;
        Vec3 dir = d.scale(1 / dist);
        // Si la cible est loin, la chaîne lâche (seul l'enroulement reste)
        boolean attached = dist < 16;
        if (attached) {
            Vec3[] n = normals(dir);
            float linkLen = 0.26f, step = 0.19f;
            double len = dist * extend;
            int count = (int) (len / step);
            for (int i = 0; i <= count; i++) {
                Vec3 p = from.add(dir.scale(i * step + linkLen / 2));
                if (i * step + linkLen / 2 > len) break;
                link(c, p, dir, i % 2 == 0 ? n[0] : n[1], linkLen, 0.13f, f.color, fade);
            }
            if (extend < 1f || tgt == null) {
                Vec3 tip = from.add(dir.scale(len));
                billboard(c, RenderType.entityTranslucent(CLAW), tip, 0.18f, age * 0.5f, f.color, fade);
            }
        }
        if (tgt == null || extend < 1f) return;
        // Enroulement : trois anneaux de maillons autour du corps
        float wrap = ease((age - 4f) / 5f);
        Vec3 base = tgt.getPosition(c.pt);
        double radius = Math.max(0.35, tgt.getBbWidth() * 0.75);
        for (int ring = 0; ring < 3; ring++) {
            double y = tgt.getBbHeight() * (0.3 + 0.22 * ring);
            int links = (int) Math.ceil(Mth.TWO_PI * radius / 0.19);
            int shown = (int) (links * wrap);
            for (int i = 0; i < shown; i++) {
                double a0 = (i + ring * 0.5) * Mth.TWO_PI / links + ring * 0.7;
                Vec3 radial = new Vec3(Math.cos(a0), 0, Math.sin(a0));
                Vec3 tangent = new Vec3(-Math.sin(a0), 0, Math.cos(a0));
                Vec3 p = base.add(radial.scale(radius)).add(0, y, 0);
                link(c, p, tangent, i % 2 == 0 ? new Vec3(0, 1, 0) : radial, 0.26f, 0.13f, f.color, fade);
            }
        }
    }

    /** Chaîne à croix enroulée autour de la main, croix qui pend et lueur de soin. */
    private static void holyChain(Ctx c, Fx f, float age) {
        Entity src = entity(f.source);
        if (src == null) return;
        Vec3 hand = hand(src, c.pt);
        float fade = age > f.duration - 6 ? (f.duration - age) / 6f : Math.min(1f, age / 3f);
        // Spirale autour du poignet
        int links = 14;
        Vec3 prev = null;
        for (int i = 0; i <= links; i++) {
            double a0 = i * 0.9 + age * 0.15;
            Vec3 p = hand.add(Math.cos(a0) * 0.16, -0.25 + i * 0.03, Math.sin(a0) * 0.16);
            if (prev != null) {
                Vec3 dir = p.subtract(prev).normalize();
                link(c, prev.lerp(p, 0.5), dir, normals(dir)[i % 2], 0.2f, 0.1f, f.color, fade);
            }
            prev = p;
        }
        // Chaîne qui pend, croix au bout, qui se balance
        double swing = Math.sin(age * 0.2) * 0.35;
        Vec3 down = new Vec3(Math.sin(swing) * 0.4, -1, 0).normalize();
        Vec3[] n = normals(down);
        for (int i = 0; i < 5; i++) {
            link(c, hand.add(down.scale(0.12 + i * 0.15)), down, n[i % 2], 0.2f, 0.1f, f.color, fade);
        }
        Vec3 tip = hand.add(down.scale(0.12 + 5 * 0.15 + 0.05));
        billboard(c, RenderType.eyes(GLOW), tip, 0.22f, 0, NenFx.HEAL, fade * (0.6f + 0.3f * Mth.sin(age * 0.5f)));
        billboard(c, RenderType.entityTranslucent(CROSS), tip, 0.1f, (float) swing, 0xFFFFFFFF, fade);
        // Halo de soin sur le corps
        if (!firstPerson(src)) billboard(c, RenderType.eyes(GLOW), center(src, c.pt), src.getBbHeight() * 0.55f, 0, NenFx.HEAL, fade * 0.35f);
    }

    /** Aiguille qui file puis reste plantée dans la cible. */
    private static void needle(Ctx c, Fx f, float age) {
        Entity src = entity(f.source);
        Entity tgt = entity(f.target);
        float flight = 2.5f;
        if (f.dir == null) {
            Vec3 from = src != null ? hand(src, c.pt) : f.at;
            Vec3 aim = tgt != null ? center(tgt, c.pt) : f.at;
            f.dir = aim.subtract(from).normalize();
            f.offset = tgt == null ? Vec3.ZERO : new Vec3((hash(f.seed, 1) - 0.5) * tgt.getBbWidth() * 0.6,
                    (hash(f.seed, 2) - 0.2) * tgt.getBbHeight() * 0.4, (hash(f.seed, 3) - 0.5) * tgt.getBbWidth() * 0.6);
        }
        Vec3 hit = tgt != null ? center(tgt, c.pt).add(f.offset) : f.at;
        Vec3 pos;
        if (age < flight) {
            Vec3 from = src != null ? hand(src, c.pt) : hit.subtract(f.dir.scale(4));
            pos = from.lerp(hit, age / flight);
        } else {
            if (tgt == null && age > flight + 6) return; // dans le vide : elle tombe
            pos = hit.subtract(f.dir.scale(0.12)); // plantée : un bout dépasse
        }
        float fade = age > f.duration - 6 ? (f.duration - age) / 6f : 1f;
        Vec3[] n = normals(f.dir);
        float len = 0.5f, w = 0.07f;
        for (Vec3 side : n) {
            Vec3 h = f.dir.scale(len / 2), s = side.scale(w / 2);
            // la tête ronde à l'arrière (gauche de la texture), la pointe devant
            quad(c, RenderType.entityTranslucent(NEEDLE), pos.subtract(h).subtract(s), pos.add(h).subtract(s),
                    pos.add(h).add(s), pos.subtract(h).add(s), 0xFFFFFFFF, fade);
        }
        if (!f.impactDone && age >= flight && tgt != null) {
            f.impactDone = true;
            PENDING.add(localFx(NenFx.Kind.IMPACT, hit, 0.6f, NenFx.MANIPULATOR, 6));
        }
    }

    /**
     * Boule de la Pierre en charge : elle grossit dans le poing, puis se recondense et change de couleur à chaque
     * palier (orange → jaune → blanc incandescent), comme dans l'anime.
     */
    private static void chargeOrb(Ctx c, Fx f, float age) {
        Entity src = entity(f.source);
        if (src == null) return;
        // Cycles : la boule grossit jusqu'à environ 2,4 blocs de large, puis repart petite avec la couleur suivante
        int[] colors = {NenFx.ENHANCER, 0xFFFFC23A, 0xFFFFF4C8, 0xFFFFFFFF};
        float cycle = 26f;
        int tier = Math.min(colors.length - 1, (int) (age / cycle));
        float inCycle = tier == colors.length - 1 ? cycle : age - tier * cycle;
        // Au sommet de chaque cycle : au moins 2× la taille de celui qui charge
        float maxHalf = Math.max(1.9f, src.getBbHeight() * 1.05f);
        float half = 0.15f + (maxHalf - 0.15f) * ease(inCycle / cycle);
        Vec3 look = src.getViewVector(c.pt);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 at;
        if (firstPerson(src)) {
            // à droite, devant : visible sans boucher la vue
            at = src.getEyePosition(c.pt).add(look.scale(6.0)).add(right.scale(2.6)).add(0, -0.2, 0);
            half *= 0.6f;
        } else {
            at = center(src, c.pt).add(right.scale(0.5 + half)).add(look.scale(0.3)).add(0, half * 0.5, 0);
        }
        int color = colors[tier];
        billboard(c, RenderType.eyes(GLOW), at, half * 2.2f, 0, color, 0.85f);
        billboard(c, RenderType.entityTranslucent(IMPACT), at, half * 1.5f, age * 0.3f, color, 0.6f + 0.1f * tier);
        billboard(c, RenderType.entityTranslucent(ORB), at, half * (1f + 0.05f * Mth.sin(age * 1.3f)), 0, color, 1f);
        // Éclat au moment où elle se recondense
        if (tier > 0 && inCycle < 4) billboard(c, RenderType.eyes(GLOW), at, 2.5f, 0, 0xFFFFFFFF, 1f - inCycle / 4f);
    }

    /** Montée d'aura : deux anneaux qui remontent le long du corps et une lueur. */
    private static void flare(Ctx c, Fx f, float t) {
        Entity src = entity(f.source);
        if (src == null) return;
        Vec3 base = src.getPosition(c.pt);
        float h = src.getBbHeight();
        float radius = Math.max(0.5f, src.getBbWidth()) * 0.95f * f.size;
        for (int k = 0; k < 2; k++) {
            float tk = t * 1.3f - k * 0.3f;
            if (tk < 0 || tk > 1) continue;
            Vec3 p = base.add(0, h * ease(tk), 0);
            float r = radius * (1.1f - 0.3f * tk);
            quad(c, RenderType.entityTranslucent(RING), p.add(-r, 0, -r), p.add(r, 0, -r), p.add(r, 0, r), p.add(-r, 0, r),
                    f.color, 1f - tk);
        }
        // Lueur sur le corps, sauf pour soi en vue subjective (elle teinterait tout l'écran)
        if (!firstPerson(src)) billboard(c, RenderType.eyes(GLOW), base.add(0, h * 0.5, 0), h * 0.8f * f.size, 0, f.color, 0.5f * (1f - t));
    }

    /**
     * Les blocs du sol sautent en vague : chaque anneau (1, 2, 3… blocs du centre) se soulève puis retombe, un peu
     * après le précédent. On dessine une copie du vrai bloc, soulevée et penchée, par-dessus.
     */
    private static void quake(Ctx c, Fx f, float age) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        int radius = Math.max(1, Math.round(f.size));
        net.minecraft.core.BlockPos center = net.minecraft.core.BlockPos.containing(f.at.x, f.at.y - 0.5, f.at.z);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius + 0.3 || d < 0.9) continue;
                float local = age - (float) d * 1.6f;
                if (local < 0 || local > 8) continue;
                net.minecraft.core.BlockPos p = surface(mc.level, center.offset(dx, 0, dz));
                if (p == null) continue;
                net.minecraft.world.level.block.state.BlockState s = mc.level.getBlockState(p);
                if (s.getRenderShape() != net.minecraft.world.level.block.RenderShape.MODEL) continue;
                int h = (p.getX() * 73856093) ^ (p.getZ() * 19349663);
                float lift = Mth.sin(local / 8f * Mth.PI) * (0.45f + 0.3f * ((h >>> 3) & 3) / 3f)
                        * (1f - (float) d / (radius + 1) * 0.5f);
                float tilt = lift * (((h >>> 7) & 1) == 0 ? 25f : -25f);
                c.pose.pushPose();
                c.pose.translate(p.getX() - c.cam.x, p.getY() - c.cam.y + lift, p.getZ() - c.cam.z);
                c.pose.translate(0.5, 0.5, 0.5);
                c.pose.mulPose(com.mojang.math.Axis.of(new Vector3f((float) -dz, 0, (float) dx).normalize()).rotationDegrees(tilt));
                c.pose.translate(-0.5, -0.5, -0.5);
                mc.getBlockRenderer().renderSingleBlock(s, c.pose, c.buffers,
                        net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level, p.above()), OverlayTexture.NO_OVERLAY);
                c.pose.popPose();
            }
        }
    }

    /** Bloc plein dont le dessus est libre, à deux blocs près de la hauteur donnée. */
    private static net.minecraft.core.BlockPos surface(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos start) {
        for (int dy = 2; dy >= -2; dy--) {
            net.minecraft.core.BlockPos p = start.offset(0, dy, 0);
            if (!level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) return p;
        }
        return null;
    }
}
