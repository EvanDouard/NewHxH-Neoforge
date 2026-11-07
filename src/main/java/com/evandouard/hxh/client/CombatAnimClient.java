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
 * Animations de combat côté client : poings droit / gauche, armement et frappe chargée, coup de pied.
 * <ul>
 *     <li>en vue extérieure (autres joueurs, PNJ, F5) : pose des bras et jambes du modèle humanoïde,
 *         appliquée après l'animation de Minecraft ({@code HumanoidModelMixin}) ;</li>
 *     <li>en vue à la première personne : les bras du joueur sont dessinés par nous pendant l'animation.</li>
 * </ul>
 */
public final class CombatAnimClient {
    private record Playing(CombatAnim anim, long start) {}

    private static final Map<Integer, Playing> PLAYING = new HashMap<>();

    private CombatAnimClient() {}

    public static void register() {
        CombatAnimPayload.clientHandler = CombatAnimClient::onPayload;
        NeoForge.EVENT_BUS.addListener(RenderHandEvent.class, CombatAnimClient::onRenderHand);
        // Frappe dans le vide : on prévient le serveur, qui décide de l'animation
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.LeftClickEmpty.class, e -> {
            if (e.getEntity().getMainHandItem().isEmpty()) PacketDistributor.sendToServer(PunchPayload.INSTANCE);
        });
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> PLAYING.clear());
    }

    private static void onPayload(CombatAnimPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        CombatAnim anim = CombatAnim.byId(payload.anim());
        if (anim == CombatAnim.STOP) PLAYING.remove(payload.entityId());
        else PLAYING.put(payload.entityId(), new Playing(anim, mc.level.getGameTime()));
    }

    /** Animation en cours d'une entité et temps écoulé (en ticks, avec la fraction), ou null. */
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

    // ---------- Courbes ----------

    /** Coup sec : sortie en {@code out} ticks, maintien {@code hold}, retour en {@code back}. Renvoie 0 → 1 → 0. */
    private static float strike(float t, float out, float hold, float back) {
        if (t < out) return ease(t / out);
        if (t < out + hold) return 1f;
        return 1f - ease(Mth.clamp((t - out - hold) / back, 0f, 1f));
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    /** Armement : rien pendant l'appui court (coup de pied possible), puis le bras recule. */
    private static float armed(float t) {
        return ease((t - com.evandouard.hxh.combat.Combat.CHARGE_MIN_TICKS) / 6f);
    }

    // ---------- Vue extérieure ----------

    /** Appelé à la fin de {@code HumanoidModel.setupAnim} pour toute entité humanoïde. */
    public static void applyToModel(HumanoidModel<?> model, LivingEntity entity, float ageInTicks, float headPitch) {
        Playing p = current(entity);
        if (p == null) return;
        float t = entity.level().getGameTime() - p.start() + (ageInTicks - entity.tickCount);
        boolean rightMain = entity.getMainArm() == HumanoidArm.RIGHT;
        ModelPart main = rightMain ? model.rightArm : model.leftArm;
        ModelPart off = rightMain ? model.leftArm : model.rightArm;
        ModelPart mainLeg = rightMain ? model.rightLeg : model.leftLeg;
        float aim = -Mth.HALF_PI - 0.1f + headPitch * Mth.DEG_TO_RAD; // bras tendu vers où l'on regarde
        float look = model.head.yRot;

        // On repart d'un buste droit (l'animation d'attaque de Minecraft le fait pivoter)
        model.body.yRot = 0f;
        model.rightArm.x = -5f;
        model.rightArm.z = 0f;
        model.leftArm.x = 5f;
        model.leftArm.z = 0f;

        switch (p.anim()) {
            case PUNCH_MAIN, PUNCH_OFF -> {
                float k = strike(t, 2f, 1f, 4f);
                ModelPart hit = p.anim() == CombatAnim.PUNCH_MAIN ? main : off;
                ModelPart guard = p.anim() == CombatAnim.PUNCH_MAIN ? off : main;
                punchArm(hit, aim, look, k);
                guardArm(guard, look, 0.8f);
                model.body.yRot = (hit == model.rightArm ? 0.25f : -0.25f) * k; // l'épaule accompagne le coup
                shoulder(model, model.body.yRot);
            }
            case CHARGE -> {
                float k = armed(t);
                float shake = k >= 1f ? Mth.sin(ageInTicks * 2.2f) * 0.04f : 0f;
                main.xRot = Mth.lerp(k, main.xRot, 0.75f + shake);
                main.yRot = Mth.lerp(k, main.yRot, 0f);
                main.zRot = Mth.lerp(k, main.zRot, rightMain ? 0.35f : -0.35f);
                guardArm(off, look, k);
                model.body.yRot = (rightMain ? -0.35f : 0.35f) * k; // buste tourné, épaule en arrière
                shoulder(model, model.body.yRot);
            }
            case CHARGED_PUNCH -> {
                float k = strike(t, 2f, 3f, 7f);
                main.xRot = 0.75f;
                main.zRot = 0f;
                punchArm(main, aim, look, k);
                off.xRot = Mth.lerp(k, -1.0f, 0.55f); // l'autre bras part en arrière pour l'élan
                off.yRot = 0f;
                model.body.yRot = (rightMain ? 0.45f : -0.45f) * k;
                shoulder(model, model.body.yRot);
            }
            case KICK -> {
                float k = strike(t, 3f, 2f, 4f);
                mainLeg.xRot = Mth.lerp(k, mainLeg.xRot, -1.45f);
                model.rightArm.zRot = Mth.lerp(k, model.rightArm.zRot, 0.5f);  // bras écartés pour l'équilibre
                model.leftArm.zRot = Mth.lerp(k, model.leftArm.zRot, -0.5f);
            }
            default -> {}
        }
    }

    private static void punchArm(ModelPart arm, float aim, float look, float k) {
        arm.xRot = Mth.lerp(k, arm.xRot, aim);
        arm.yRot = Mth.lerp(k, arm.yRot, look);
        arm.zRot = Mth.lerp(k, arm.zRot, 0f);
    }

    /** Garde : bras levé devant le visage. */
    private static void guardArm(ModelPart arm, float look, float k) {
        arm.xRot = Mth.lerp(k, arm.xRot, -1.2f);
        arm.yRot = Mth.lerp(k, arm.yRot, look);
        arm.zRot = Mth.lerp(k, arm.zRot, 0f);
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
                float k = strike(t, 2f, 1f, 4f);
                HumanoidArm hit = p.anim() == CombatAnim.PUNCH_MAIN ? mainSide : offSide;
                arm(renderer, event, hit, k, 0f, 0.45f);
                if (hit == offSide) arm(renderer, event, mainSide, 0f, 0f, 0f);
            }
            case CHARGE -> arm(renderer, event, mainSide, 0f, armed(t), 0f);
            case CHARGED_PUNCH -> {
                float k = strike(t, 2f, 3f, 7f);
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
