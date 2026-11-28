package com.evandouard.hxh.client;

import com.evandouard.hxh.combat.CombatAnim;
import com.evandouard.hxh.network.CombatAnimPayload;
import com.evandouard.hxh.network.PunchPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

/**
 * Animations de combat côté client, à la manière d'Epic Fight : chaque coup est une suite de poses du corps entier
 * ({@link CombatPose}) — anticipation, impact explosif avec torsion du buste et appui des jambes, prolongement du
 * geste, retour amorti à la garde — et un nouveau coup part de la pose où l'on se trouve (enchaînements sans saut).
 * <ul>
 *     <li>en vue extérieure (autres joueurs, PNJ, F5) : pose du modèle humanoïde, appliquée après l'animation de
 *         Minecraft ({@code HumanoidModelMixin}), plus la descente et l'avancée du corps au rendu ;</li>
 *     <li>en vue à la première personne : les bras du joueur sont dessinés par nous pendant l'animation.</li>
 * </ul>
 */
public final class CombatAnimClient {
    /** Animation en cours, son départ (temps de jeu) et la pose d'où elle part (fin du coup précédent). */
    private record Playing(CombatAnim anim, long start, CombatPose from) {}

    /** Durée du fondu depuis la pose précédente quand un coup en remplace un autre (ticks). */
    private static final float CROSSFADE = 2.0f;

    private static final Map<Integer, Playing> PLAYING = new HashMap<>();

    private CombatAnimClient() {}

    public static void register() {
        CombatAnimPayload.clientHandler = CombatAnimClient::onPayload;
        NeoForge.EVENT_BUS.addListener(RenderHandEvent.class, CombatAnimClient::onRenderHand);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, false,
                net.neoforged.neoforge.client.event.RenderLivingEvent.Pre.class, CombatAnimClient::onPre);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                net.neoforged.neoforge.client.event.RenderLivingEvent.Post.class, CombatAnimClient::onPost);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> PLAYING.clear());
    }

    private static void onPayload(CombatAnimPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        CombatAnim anim = CombatAnim.byId(payload.anim());
        // la pose actuelle (coup précédent en cours) devient le point de départ du nouveau coup
        CombatPose from = null;
        Playing prev = PLAYING.get(payload.entityId());
        if (prev != null && mc.level.getEntity(payload.entityId()) instanceof LivingEntity e) {
            from = evaluate(prev, e, mc.level.getGameTime() - prev.start());
        }
        if (anim == CombatAnim.STOP) PLAYING.remove(payload.entityId());
        else PLAYING.put(payload.entityId(), new Playing(anim, mc.level.getGameTime(), from));
    }

    /** Animation en cours d'une entité, ou null. */
    private static Playing current(LivingEntity entity) {
        Playing p = PLAYING.get(entity.getId());
        if (p == null || entity.level() == null) return null;
        if (entity.level().getGameTime() - p.start() > p.anim().duration) {
            PLAYING.remove(entity.getId());
            return null;
        }
        return p;
    }

    /** Dernière animation reçue pour une entité (tests du client scripté). */
    public static CombatAnim playing(int entityId) {
        Playing p = PLAYING.get(entityId);
        return p == null ? null : p.anim();
    }

    // ---------- Les coups ----------

    private static final float HP = Mth.HALF_PI;

    /** Garde de base (droitier) : poings devant le menton, pied gauche devant, genoux souples. */
    private static CombatPose guard() {
        return new CombatPose().body(-0.1f, 0.03f)
                .rArm(-1.25f, -0.3f, 0f).lArm(-1.35f, 0.35f, 0f)
                .rLeg(0.3f, 0f, 0.06f).lLeg(-0.28f, 0f, -0.06f)
                .elbows(-1.15f, -1.25f).knees(0.2f, 0.3f)
                .move(0.05f, 0f);
    }

    private static CombatPose.Key key(float t, CombatPose p, CombatPose.Ease e) {
        return new CombatPose.Key(t, p, e);
    }

    /** Cross du bras arrière (droit) : armement court, rotation des hanches et des épaules, appui arrière qui pivote. */
    private static final CombatPose.Key[] CROSS = {
            key(0f, guard().weight(0f), CombatPose.Ease.LINEAR),
            key(1.5f, guard().body(-0.24f, 0.05f).rArm(-1.0f, -0.12f, 0.25f).elbows(-1.4f, -1.25f).knees(0.3f, 0.35f).move(0.09f, -0.02f), CombatPose.Ease.SMOOTH),
            key(3.0f, guard().body(0.55f, 0.14f).nod(0.05f).rArm(-HP - 0.08f, -0.12f, 0f).aim(1f, 0f)
                    .lArm(-1.45f, 0.42f, 0f).rLeg(0.55f, 0f, 0.08f).lLeg(-0.36f, 0f, -0.06f).move(0.07f, 0.16f)
                    .elbows(0f, -1.35f).knees(0.05f, 0.45f),
                    CombatPose.Ease.SNAP),
            key(4.5f, guard().body(0.62f, 0.16f).nod(0.05f).rArm(-HP - 0.12f, -0.18f, 0f).aim(1f, 0f)
                    .lArm(-1.45f, 0.42f, 0f).rLeg(0.6f, 0f, 0.08f).lLeg(-0.38f, 0f, -0.06f).move(0.07f, 0.2f)
                    .elbows(0f, -1.35f).knees(0.05f, 0.5f),
                    CombatPose.Ease.LINEAR),
            key(10f, guard().weight(0f), CombatPose.Ease.RECOVER),
    };

    /** Direct du bras avant (gauche) : rapide, presque sans armement, l'épaule avant pousse. */
    private static final CombatPose.Key[] JAB = {
            key(0f, guard().weight(0f), CombatPose.Ease.LINEAR),
            key(1.0f, guard().body(-0.04f, 0.04f).lArm(-1.2f, 0.3f, -0.1f).move(0.07f, 0f), CombatPose.Ease.SMOOTH),
            key(2.2f, guard().body(-0.32f, 0.09f).lArm(-HP - 0.06f, 0.12f, 0f).aim(0f, 1f)
                    .rArm(-1.4f, -0.36f, 0f).lLeg(-0.42f, 0f, -0.06f).rLeg(0.36f, 0f, 0.06f).move(0.06f, 0.11f)
                    .elbows(-1.3f, 0f).knees(0.25f, 0.4f),
                    CombatPose.Ease.SNAP),
            key(3.4f, guard().body(-0.36f, 0.1f).lArm(-HP - 0.1f, 0.16f, 0f).aim(0f, 1f)
                    .rArm(-1.4f, -0.36f, 0f).lLeg(-0.42f, 0f, -0.06f).rLeg(0.36f, 0f, 0.06f).move(0.06f, 0.13f)
                    .elbows(-1.3f, 0f).knees(0.25f, 0.4f),
                    CombatPose.Ease.LINEAR),
            key(9f, guard().weight(0f), CombatPose.Ease.RECOVER),
    };

    /** Frappe armée : garde basse et large, bras droit loin derrière, bras gauche tendu vers la cible. */
    private static CombatPose windUp() {
        return new CombatPose().body(-0.65f, 0.1f).nod(0.08f)
                .rArm(0.55f, 0.1f, 0.55f).lArm(-1.45f, 0.25f, 0f).aim(0f, 0.8f)
                .rLeg(0.7f, 0f, 0.15f).lLeg(-0.55f, 0f, -0.12f)
                .elbows(-1.5f, -0.2f).knees(0.55f, 0.75f)
                .move(0.2f, -0.04f);
    }

    private static final CombatPose.Key[] CHARGE = {
            key(0f, guard().weight(0f), CombatPose.Ease.LINEAR),
            key(com.evandouard.hxh.combat.Combat.CHARGE_MIN_TICKS, guard().weight(0f), CombatPose.Ease.LINEAR),
            key(com.evandouard.hxh.combat.Combat.CHARGE_MIN_TICKS + 2.5f, guard().move(0.09f, 0f), CombatPose.Ease.SMOOTH),
            key(com.evandouard.hxh.combat.Combat.CHARGE_MIN_TICKS + 8f, windUp(), CombatPose.Ease.SMOOTH),
    };

    /** Frappe chargée relâchée : fente explosive, rotation complète, bras gauche rejeté en arrière, long prolongement. */
    private static final CombatPose.Key[] CHARGED = {
            key(0f, windUp(), CombatPose.Ease.LINEAR),
            key(2.5f, new CombatPose().body(0.8f, 0.28f).nod(0.1f)
                    .rArm(-HP - 0.05f, -0.1f, 0f).aim(1f, 0f).lArm(0.7f, 0.2f, -0.35f)
                    .rLeg(0.85f, 0f, 0.1f).lLeg(-0.7f, 0f, -0.1f).move(0.14f, 0.5f).elbows(0f, -0.5f).knees(0.05f, 0.8f), CombatPose.Ease.SNAP),
            key(5f, new CombatPose().body(0.9f, 0.3f).nod(0.1f)
                    .rArm(-HP - 0.15f, -0.14f, 0f).aim(1f, 0f).lArm(0.8f, 0.2f, -0.4f)
                    .rLeg(0.9f, 0f, 0.1f).lLeg(-0.72f, 0f, -0.1f).move(0.14f, 0.58f).elbows(0f, -0.5f).knees(0.05f, 0.85f), CombatPose.Ease.LINEAR),
            key(8f, new CombatPose().body(0.85f, 0.26f).nod(0.08f)
                    .rArm(-HP - 0.1f, -0.12f, 0f).aim(1f, 0f).lArm(0.6f, 0.2f, -0.3f)
                    .rLeg(0.85f, 0f, 0.1f).lLeg(-0.7f, 0f, -0.1f).move(0.13f, 0.58f).elbows(-0.2f, -0.6f).knees(0.1f, 0.8f), CombatPose.Ease.LINEAR),
            key(16f, guard().weight(0f), CombatPose.Ease.RECOVER),
    };

    /** Coup de pied circulaire (jambe droite) : jambe armée sur le côté, hanches qui tournent, buste en contrepoids. */
    private static final CombatPose.Key[] KICK = {
            key(0f, guard().weight(0f), CombatPose.Ease.LINEAR),
            key(2.5f, guard().body(-0.3f, -0.12f).rLeg(-1.2f, 0.8f, 0.5f).lLeg(0.05f, 0f, -0.08f)
                    .rArm(0.3f, 0f, 0.35f).lArm(-1.3f, 0.4f, 0f).move(-0.02f, 0f)
                    .elbows(-0.3f, -1.3f).knees(1.5f, 0.15f), CombatPose.Ease.SMOOTH),
            key(4.5f, guard().body(0.45f, -0.22f).roll(-0.08f).rLeg(-1.5f, -0.25f, 0.25f).lLeg(0.1f, 0f, -0.1f)
                    .rArm(0.6f, 0f, 0.5f).lArm(-1.0f, 0.6f, -0.2f).move(-0.03f, 0.08f)
                    .elbows(-0.2f, -1.1f).knees(0f, 0.2f), CombatPose.Ease.SNAP),
            key(6.5f, guard().body(0.6f, -0.2f).roll(-0.08f).rLeg(-1.45f, -0.5f, 0.2f).lLeg(0.1f, 0f, -0.1f)
                    .rArm(0.6f, 0f, 0.5f).lArm(-1.0f, 0.6f, -0.2f).move(-0.02f, 0.1f)
                    .elbows(-0.2f, -1.1f).knees(0.1f, 0.2f), CombatPose.Ease.LINEAR),
            key(12f, guard().weight(0f), CombatPose.Ease.RECOVER),
    };

    /** Pose du coup {@code p} à l'instant {@code t}, dans le bon sens pour l'entité (droitier / gaucher). */
    private static CombatPose evaluate(Playing p, LivingEntity entity, float t) {
        boolean rightMain = entity.getMainArm() == HumanoidArm.RIGHT;
        CombatPose pose = switch (p.anim()) {
            // le coup de la main principale est le cross ; l'autre main lance le direct
            case PUNCH_MAIN -> CombatPose.sample(CROSS, t);
            case PUNCH_OFF -> CombatPose.sample(JAB, t);
            case CHARGE -> CombatPose.sample(CHARGE, t);
            case CHARGED_PUNCH -> CombatPose.sample(CHARGED, t);
            case KICK -> CombatPose.sample(KICK, t);
            default -> guard().weight(0f);
        };
        if (!rightMain) pose = pose.mirror();
        if (p.from() != null && t < CROSSFADE) pose = CombatPose.lerp(p.from(), pose, CombatPose.Ease.SMOOTH.apply(t / CROSSFADE));
        return pose;
    }

    private static float time(Playing p, LivingEntity entity, float ageInTicks) {
        return entity.level().getGameTime() - p.start() + (ageInTicks - entity.tickCount);
    }

    // ---------- Vue extérieure ----------

    /** Appelé à la fin de {@code HumanoidModel.setupAnim} pour toute entité humanoïde. */
    public static void applyToModel(HumanoidModel<?> model, LivingEntity entity, float ageInTicks, float headPitch) {
        Playing p = current(entity);
        if (p == null) return;
        float t = time(p, entity, ageInTicks);
        CombatPose pose = evaluate(p, entity, t);
        if (p.anim() == CombatAnim.CHARGE && t > com.evandouard.hxh.combat.Combat.CHARGE_MIN_TICKS + 8f) {
            // armé et tendu : le corps tremble sous l'aura contenue
            pose = pose.copy();
            pose.twist += Mth.sin(ageInTicks * 2.3f) * 0.02f;
            float sh = Mth.sin(ageInTicks * 3.1f) * 0.035f;
            pose.rArm[0] += sh;
            pose.lArm[0] += sh;
        }
        float w = Mth.clamp(pose.weight, 0f, 1f);
        if (w <= 0.001f) return;
        float pitch = headPitch * Mth.DEG_TO_RAD;
        float look = model.head.yRot;

        // buste : torsion, inclinaison (avec le décalage des membres, comme l'accroupi de Minecraft), roulis
        float lean = pose.lean * w;
        float twist = pose.twist * w;
        model.body.yRot = twist;
        model.body.xRot = Mth.lerp(w, model.body.xRot, pose.lean);
        model.body.zRot = Mth.lerp(w, model.body.zRot, pose.roll);
        model.body.y += 6.4f * lean;
        model.head.y += 8.4f * lean;
        model.head.xRot += pose.nod * w;
        model.rightArm.y = 2f + 6.4f * lean;
        model.leftArm.y = 2f + 6.4f * lean;
        model.rightLeg.z += 8f * lean;
        model.leftLeg.z += 8f * lean;
        model.rightLeg.y += 0.4f * lean;
        model.leftLeg.y += 0.4f * lean;

        limb(model.rightArm, pose.rArm, w, pose.rAim, pitch, look);
        limb(model.leftArm, pose.lArm, w, pose.lAim, pitch, look);
        limb(model.rightLeg, pose.rLeg, w, 0f, 0f, 0f);
        limb(model.leftLeg, pose.lLeg, w, 0f, 0f, 0f);
        shoulder(model, twist);
        // un bras qui vise garde sa direction : il compense la torsion du buste (le poing va droit sur la cible)
        model.rightArm.yRot -= pose.rAim * twist;
        model.leftArm.yRot -= pose.lAim * twist;
    }

    private static void limb(ModelPart part, float[] r, float w, float aim, float pitch, float look) {
        part.xRot = Mth.lerp(w, part.xRot, r[0] + aim * pitch);
        part.yRot = Mth.lerp(w, part.yRot, r[1] + aim * look);
        part.zRot = Mth.lerp(w, part.zRot, r[2]);
    }

    /** Replace les épaules quand le buste pivote (comme le fait Minecraft pour son animation d'attaque). */
    private static void shoulder(HumanoidModel<?> model, float bodyYaw) {
        model.rightArm.z = Mth.sin(bodyYaw) * 5f;
        model.rightArm.x = -Mth.cos(bodyYaw) * 5f;
        model.leftArm.z = -Mth.sin(bodyYaw) * 5f;
        model.leftArm.x = Mth.cos(bodyYaw) * 5f;
        model.rightArm.yRot += bodyYaw;
        model.leftArm.yRot += bodyYaw;
    }

    // ---------- Personnages GeckoLib (os : torse, bras, avant-bras, jambes, tibias) ----------

    /**
     * Applique le coup en cours aux os d'un personnage GeckoLib (appelé depuis {@code setCustomAnimations}, après
     * les contrôleurs) : torsion et inclinaison du torse, bras et coudes, jambes et genoux, descente et avancée du
     * corps. Les bras et la tête étant enfants du torse, un bras qui vise et la tête compensent sa torsion.
     *
     * @return vrai si un coup est en cours (la tête a alors déjà été orientée)
     */
    public static boolean applyToBones(software.bernie.geckolib.animation.AnimationProcessor<?> bones, LivingEntity entity,
                                       float partialTick, float headPitchDeg, float netHeadYawDeg) {
        Playing p = current(entity);
        if (p == null) return false;
        float t = entity.level().getGameTime() - p.start() + partialTick;
        CombatPose pose = evaluate(p, entity, t);
        if (p.anim() == CombatAnim.CHARGE && t > com.evandouard.hxh.combat.Combat.CHARGE_MIN_TICKS + 8f) {
            pose = pose.copy();
            float age = entity.tickCount + partialTick;
            pose.twist += Mth.sin(age * 2.3f) * 0.02f;
            pose.rArm[0] += Mth.sin(age * 3.1f) * 0.035f;
        }
        float w = Mth.clamp(pose.weight, 0f, 1f);
        if (w <= 0.001f) return false;
        float pitch = headPitchDeg * Mth.DEG_TO_RAD, look = netHeadYawDeg * Mth.DEG_TO_RAD;
        var torso = bones.getBone("torso");
        var body = bones.getBone("body");
        var head = bones.getBone("head");
        float twist = pose.twist;
        if (torso != null) {
            torso.setRotY(Mth.lerp(w, torso.getRotY(), twist));
            torso.setRotX(Mth.lerp(w, torso.getRotX(), pose.lean));
            torso.setRotZ(Mth.lerp(w, torso.getRotZ(), pose.roll));
        }
        if (head != null) {
            head.setRotX(pitch + pose.nod * w - pose.lean * w);
            head.setRotY(look - twist * w);
        }
        if (body != null) {
            body.setPosY(Mth.lerp(w, body.getPosY(), -pose.sink * 16f));
            body.setPosZ(Mth.lerp(w, body.getPosZ(), -pose.lunge * 16f));
        }
        geoArm(bones, "right", pose.rArm, pose.rElbow, pose.rAim, pitch, look - twist, w);
        geoArm(bones, "left", pose.lArm, pose.lElbow, pose.lAim, pitch, look - twist, w);
        geoLeg(bones, "right", pose.rLeg, pose.rKnee, w);
        geoLeg(bones, "left", pose.lLeg, pose.lKnee, w);
        return true;
    }

    private static void geoArm(software.bernie.geckolib.animation.AnimationProcessor<?> bones, String side, float[] r,
                               float elbow, float aim, float pitch, float lookRel, float w) {
        var arm = bones.getBone(side + "_arm");
        var fore = bones.getBone(side + "_forearm");
        if (arm == null) return;
        // un coude plié abaisse le bras : la main reste à la même hauteur que le bras tendu du modèle simple
        float x = r[0] + aim * pitch + 0.5f * Math.abs(elbow);
        float rest = arm.getInitialSnapshot().getRotZ();
        arm.setRotX(Mth.lerp(w, arm.getRotX(), x));
        arm.setRotY(Mth.lerp(w, arm.getRotY(), r[1] + aim * lookRel));
        arm.setRotZ(Mth.lerp(w, arm.getRotZ(), rest + r[2]));
        if (fore != null) fore.setRotX(Mth.lerp(w, fore.getRotX(), elbow));
    }

    private static void geoLeg(software.bernie.geckolib.animation.AnimationProcessor<?> bones, String side, float[] r,
                               float knee, float w) {
        var leg = bones.getBone(side + "_leg");
        var shin = bones.getBone(side + "_shin");
        if (leg == null) return;
        float rest = leg.getInitialSnapshot().getRotZ();
        // un genou plié fait avancer la cuisse d'autant pour garder le pied sous le corps
        leg.setRotX(Mth.lerp(w, leg.getRotX(), r[0] - 0.5f * knee));
        leg.setRotY(Mth.lerp(w, leg.getRotY(), r[1]));
        leg.setRotZ(Mth.lerp(w, leg.getRotZ(), rest + r[2]));
        if (shin != null) shin.setRotX(Mth.lerp(w, shin.getRotX(), knee));
    }

    // ---------- Descente et avancée du corps au rendu ----------

    private static final Map<Integer, Boolean> PUSHED = new HashMap<>();

    private static void onPre(net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity e = event.getEntity();
        if (!(event.getRenderer().getModel() instanceof HumanoidModel<?>)) return;
        Playing p = current(e);
        if (p == null) return;
        float t = time(p, e, e.tickCount + event.getPartialTick());
        CombatPose pose = evaluate(p, e, t);
        float w = Mth.clamp(pose.weight, 0f, 1f);
        if (w <= 0.001f || (Math.abs(pose.sink) < 1e-3f && Math.abs(pose.lunge) < 1e-3f)) return;
        float yaw = Mth.rotLerp(event.getPartialTick(), e.yBodyRotO, e.yBodyRot) * Mth.DEG_TO_RAD;
        float s = e.getScale() * w;
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(-Mth.sin(yaw) * pose.lunge * s, -pose.sink * s, Mth.cos(yaw) * pose.lunge * s);
        PUSHED.put(e.getId(), true);
    }

    private static void onPost(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event) {
        if (PUSHED.remove(event.getEntity().getId()) != null) event.getPoseStack().popPose();
    }

    // ---------- Courbes de la vue subjective ----------

    /** Coup en vue subjective : léger recul ({@code wind} ticks), sortie explosive, maintien, retour amorti. */
    private static float strike(float t, float wind, float out, float hold, float back) {
        if (t < wind) return -0.18f * CombatPose.Ease.SMOOTH.apply(t / wind);
        t -= wind;
        if (t < out) return Mth.lerp(CombatPose.Ease.SNAP.apply(t / out), -0.18f, 1f);
        if (t < out + hold) return 1f;
        return 1f - CombatPose.Ease.RECOVER.apply((t - out - hold) / back);
    }

    /** Armement : rien pendant l'appui court (coup de pied possible), puis le bras recule. */
    private static float armed(float t) {
        return CombatPose.Ease.SMOOTH.apply((t - com.evandouard.hxh.combat.Combat.CHARGE_MIN_TICKS) / 8f);
    }

    // ---------- Vue à la première personne ----------

    private static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getItemStack().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || player.isInvisible()) return;
        Playing p = current(player);
        if (p == null) return;
        event.setCanceled(true);
        float t = player.level().getGameTime() - p.start() + event.getPartialTick();
        HumanoidArm mainSide = player.getMainArm();
        HumanoidArm offSide = mainSide.getOpposite();
        var renderer = mc.getEntityRenderDispatcher().getItemInHandRenderer();
        PoseStack pose = event.getPoseStack();

        switch (p.anim()) {
            case PUNCH_MAIN, PUNCH_OFF -> {
                float k = p.anim() == CombatAnim.PUNCH_MAIN ? strike(t, 1.5f, 1.5f, 1.5f, 5.5f) : strike(t, 1f, 1.2f, 1.2f, 5.6f);
                HumanoidArm hit = p.anim() == CombatAnim.PUNCH_MAIN ? mainSide : offSide;
                arm(renderer, event, hit, k, 0f, 0.45f);
                if (hit == offSide) arm(renderer, event, mainSide, 0f, 0f, 0f);
            }
            case CHARGE -> arm(renderer, event, mainSide, 0f, armed(t), 0f);
            case CHARGED_PUNCH -> {
                float k = Math.max(0f, strike(t, 0f, 2.5f, 5.5f, 8f));
                arm(renderer, event, mainSide, k, Math.max(0f, 1f - t / 2f), 0.7f);
            }
            case KICK -> arm(renderer, event, mainSide, 0f, 0f, 0f); // la jambe ne se voit pas en vue subjective
            default -> {}
        }
    }

    /**
     * Dessine un bras en vue subjective. {@code thrust} : coup vers l'avant (0 → 1), {@code back} : bras armé en
     * arrière (0 → 1), {@code reach} : distance du coup.
     */
    private static void arm(net.minecraft.client.renderer.ItemInHandRenderer renderer, RenderHandEvent event,
                            HumanoidArm side, float thrust, float back, float reach) {
        PoseStack pose = event.getPoseStack();
        float f = side == HumanoidArm.RIGHT ? 1f : -1f;
        pose.pushPose();
        // coup : le poing part vers le centre de l'écran et vers l'avant
        pose.translate(-f * 0.22f * thrust, 0.12f * thrust, -reach * thrust);
        pose.mulPose(Axis.YP.rotationDegrees(f * 12f * thrust));
        // armement : le bras recule et descend, avec un léger tremblement quand la barre tourne
        float shake = back >= 1f ? Mth.sin((Minecraft.getInstance().player.tickCount + event.getPartialTick()) * 2.2f) * 0.01f : 0f;
        pose.translate(f * 0.1f * back + shake, -0.18f * back, 0.28f * back);
        pose.mulPose(Axis.XP.rotationDegrees(25f * back));
        renderer.renderPlayerArm(pose, event.getMultiBufferSource(), event.getPackedLight(), 0f, 0f, side);
        pose.popPose();
    }
}
