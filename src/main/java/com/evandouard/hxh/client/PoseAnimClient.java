package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;

/**
 * Poses tenues du corps, côté client, pour toute entité humanoïde (joueurs, PNJ humanoïdes) — l'état vient des
 * données Hunter synchronisées, donc les autres joueurs voient la même pose.
 * <ul>
 *     <li><b>Méditation</b> : assis en tailleur, jambes croisées devant soi, mains jointes au creux des jambes, dos
 *         droit, tête à peine inclinée ; respiration lente (le buste se soulève, les épaules suivent).</li>
 *     <li><b>Prière de gratitude</b> (Netero) : à genoux, buste droit, paumes jointes devant la poitrine (gasshō) ;
 *         toutes les six secondes, une inclinaison lente de la tête et des mains, puis le retour.</li>
 * </ul>
 * On entre et on sort de la pose en douceur ({@link #BLEND} ticks, courbe adoucie) au lieu de sauter d'une pose à
 * l'autre ; le corps descend en même temps vers le sol (décalage du rendu, voir {@link #onPre}).
 */
public final class PoseAnimClient {
    /** Durée de l'entrée / de la sortie de pose, en ticks. */
    static final float BLEND = 14f;
    /** Descente du corps : assis en tailleur, à genoux (en blocs). */
    static final float DROP_SIT = 0.56f, DROP_KNEEL = 0.34f;

    enum Kind { NONE, MEDITATE, PRAY }

    /** Pose demandée, pose précédente (pour la sortie en douceur) et instant du changement (âge de l'entité). */
    private record State(Kind kind, Kind previous, float since) {}

    private static final Map<Integer, State> STATES = new HashMap<>();

    private PoseAnimClient() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, RenderLivingEvent.Pre.class, PoseAnimClient::onPre);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RenderLivingEvent.Post.class, PoseAnimClient::onPost);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> STATES.clear());
    }

    private static Kind wanted(LivingEntity e) {
        if (!e.hasData(ModAttachments.HUNTER)) return Kind.NONE;
        HunterData d = e.getData(ModAttachments.HUNTER);
        if (!d.meditating()) return Kind.NONE;
        return com.evandouard.hxh.hatsu.Netero.isNetero(e) ? Kind.PRAY : Kind.MEDITATE;
    }

    /** Met à jour l'état et renvoie {pose, poids 0..1} pour cette image. */
    private static State state(LivingEntity e, float age) {
        Kind want = wanted(e);
        State s = STATES.get(e.getId());
        if (s == null) {
            if (want == Kind.NONE) return null;
            s = new State(want, Kind.NONE, age);
            STATES.put(e.getId(), s);
        } else if (s.kind() != want) {
            // on repart de la pose en cours, quel que soit l'avancement de la transition précédente
            s = new State(want, s.kind(), age);
            STATES.put(e.getId(), s);
        }
        if (s.kind() == Kind.NONE && age - s.since() > BLEND) {
            STATES.remove(e.getId());
            return null;
        }
        return s;
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * x * (x * (x * 6f - 15f) + 10f); // smootherstep : départ et arrivée sans à-coup
    }

    /** Poids de la pose {@code kind} : monte quand on y entre, descend quand on en sort. */
    private static float weight(State s, Kind kind, float age) {
        float k = ease((age - s.since()) / BLEND);
        if (s.kind() == kind) return s.previous() == kind ? 1f : k;
        if (s.previous() == kind) return 1f - k;
        return 0f;
    }

    // ---------- Descente du corps ----------

    private static final Map<Integer, Boolean> PUSHED = new HashMap<>();

    private static void onPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity e = event.getEntity();
        float age = e.tickCount + event.getPartialTick();
        State s = state(e, age);
        if (s == null || !(event.getRenderer().getModel() instanceof HumanoidModel<?>)) return;
        float drop = DROP_SIT * weight(s, Kind.MEDITATE, age) + DROP_KNEEL * weight(s, Kind.PRAY, age);
        if (drop <= 0.001f) return;
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(0, -drop * e.getScale(), 0);
        PUSHED.put(e.getId(), true);
    }

    private static void onPost(RenderLivingEvent.Post<?, ?> event) {
        if (PUSHED.remove(event.getEntity().getId()) != null) event.getPoseStack().popPose();
    }

    // ---------- Pose des membres ----------

    /** Appelé à la fin de {@code HumanoidModel.setupAnim}, après les animations de combat. */
    public static void applyToModel(HumanoidModel<?> model, LivingEntity entity, float ageInTicks) {
        State s = STATES.get(entity.getId());
        if (s == null) s = state(entity, ageInTicks);
        if (s == null) return;
        float med = weight(s, Kind.MEDITATE, ageInTicks);
        float pray = weight(s, Kind.PRAY, ageInTicks);
        if (med > 0f) meditate(model, ageInTicks, med);
        if (pray > 0f) pray(model, ageInTicks, pray);
    }

    private static void set(ModelPart p, float k, float x, float y, float z) {
        p.xRot = Mth.lerp(k, p.xRot, x);
        p.yRot = Mth.lerp(k, p.yRot, y);
        p.zRot = Mth.lerp(k, p.zRot, z);
    }

    /** Respiration : 0..1..0 sur environ 4 s. */
    private static float breath(float age) {
        return 0.5f - 0.5f * Mth.cos(age * Mth.TWO_PI / 80f);
    }

    private static void meditate(HumanoidModel<?> m, float age, float k) {
        float b = breath(age);
        // jambes croisées devant : à l'horizontale, chaque pied passe devant l'autre jambe
        set(m.rightLeg, k, -Mth.HALF_PI + 0.06f, -0.62f, 0.05f);
        set(m.leftLeg, k, -Mth.HALF_PI + 0.02f, 0.62f, -0.05f);
        m.rightLeg.y = Mth.lerp(k, m.rightLeg.y, 11.4f);   // la jambe droite passe au-dessus : pas de chevauchement
        m.leftLeg.y = Mth.lerp(k, m.leftLeg.y, 12.2f);
        m.rightLeg.z = Mth.lerp(k, m.rightLeg.z, -0.4f);
        m.leftLeg.z = Mth.lerp(k, m.leftLeg.z, 0.4f);
        // buste droit qui respire, épaules qui suivent
        float lift = -0.35f * b;
        m.body.xRot = Mth.lerp(k, m.body.xRot, 0f);
        m.body.yRot = Mth.lerp(k, m.body.yRot, 0f);
        m.body.y = Mth.lerp(k, m.body.y, lift);
        // tête à peine baissée, yeux vers le sol devant soi ; garde un peu du regard du joueur
        m.head.xRot = Mth.lerp(k, m.head.xRot, 0.22f + 0.04f * b + m.head.xRot * 0.15f);
        m.head.yRot = Mth.lerp(k, m.head.yRot, m.head.yRot * 0.2f);
        m.head.zRot = Mth.lerp(k, m.head.zRot, 0f);
        m.head.y = Mth.lerp(k, m.head.y, lift);
        // mains jointes au creux des jambes : avant-bras vers le bas et vers le centre
        set(m.rightArm, k, -0.52f - 0.04f * b, -0.12f, -0.36f);
        set(m.leftArm, k, -0.52f - 0.04f * b, 0.12f, 0.36f);
        m.rightArm.x = Mth.lerp(k, m.rightArm.x, -5f);
        m.leftArm.x = Mth.lerp(k, m.leftArm.x, 5f);
        m.rightArm.y = Mth.lerp(k, m.rightArm.y, 2f + lift);
        m.leftArm.y = Mth.lerp(k, m.leftArm.y, 2f + lift);
        m.rightArm.z = Mth.lerp(k, m.rightArm.z, 0f);
        m.leftArm.z = Mth.lerp(k, m.leftArm.z, 0f);
        copyHat(m);
    }

    /** Inclinaison de la prière : 0 la plupart du temps, monte puis redescend lentement toutes les 6 s. */
    private static float bow(float age) {
        float t = age % 120f;
        if (t > 50f) return 0f;
        return Mth.sin(t / 50f * Mth.PI) * Mth.sin(t / 50f * Mth.PI);
    }

    private static void pray(HumanoidModel<?> m, float age, float k) {
        float b = breath(age);
        float w = bow(age);
        // à genoux : cuisses qui descendent en biais vers l'arrière jusqu'au sol, à peine écartées
        set(m.rightLeg, k, 1.0f, 0.04f, 0.04f);
        set(m.leftLeg, k, 1.0f, -0.04f, -0.04f);
        m.rightLeg.y = Mth.lerp(k, m.rightLeg.y, 12f);
        m.leftLeg.y = Mth.lerp(k, m.leftLeg.y, 12f);
        m.rightLeg.z = Mth.lerp(k, m.rightLeg.z, 0.6f);
        m.leftLeg.z = Mth.lerp(k, m.leftLeg.z, 0.6f);
        float lift = -0.25f * b;
        m.body.xRot = Mth.lerp(k, m.body.xRot, 0f);
        m.body.yRot = Mth.lerp(k, m.body.yRot, 0f);
        m.body.y = Mth.lerp(k, m.body.y, lift);
        // tête : s'incline avec la prière
        m.head.xRot = Mth.lerp(k, m.head.xRot, 0.12f + 0.55f * w);
        m.head.yRot = Mth.lerp(k, m.head.yRot, 0f);
        m.head.zRot = Mth.lerp(k, m.head.zRot, 0f);
        m.head.y = Mth.lerp(k, m.head.y, lift + 0.8f * w);
        // paumes jointes devant la poitrine (gasshō) : les mains se rejoignent au centre, doigts vers le haut ;
        // pendant l'inclinaison elles descendent un peu vers le front baissé
        float ax = -1.2f + 0.22f * w;
        set(m.rightArm, k, ax, -0.55f, 0f);
        set(m.leftArm, k, ax, 0.55f, 0f);
        m.rightArm.x = Mth.lerp(k, m.rightArm.x, -4.2f);
        m.leftArm.x = Mth.lerp(k, m.leftArm.x, 4.2f);
        m.rightArm.y = Mth.lerp(k, m.rightArm.y, 2f + lift + 0.6f * w);
        m.leftArm.y = Mth.lerp(k, m.leftArm.y, 2f + lift + 0.6f * w);
        m.rightArm.z = Mth.lerp(k, m.rightArm.z, -0.5f);
        m.leftArm.z = Mth.lerp(k, m.leftArm.z, -0.5f);
        copyHat(m);
    }

    private static void copyHat(HumanoidModel<?> m) {
        m.hat.copyFrom(m.head);
    }

    /** Pose en cours d'une entité (client scripté de la CI). */
    public static String current(int entityId) {
        State s = STATES.get(entityId);
        return s == null ? "none" : s.kind().name().toLowerCase(java.util.Locale.ROOT);
    }
}
