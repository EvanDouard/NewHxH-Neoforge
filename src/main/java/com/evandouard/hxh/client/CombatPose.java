package com.evandouard.hxh.client;

import net.minecraft.util.Mth;

/**
 * Pose du corps entier pour les animations de combat (à la manière d'Epic Fight) : torsion et inclinaison du buste,
 * bras et jambes, descente et avancée du corps. Les angles sont en radians, dans le repère du {@code HumanoidModel}
 * (bras/jambes : x négatif = vers l'avant), pour un coup donné du <b>bras droit</b> ; les animations du bras gauche
 * sont obtenues par {@link #mirror()}.
 */
final class CombatPose {
    /** Torsion du buste (positif : l'épaule droite avance), inclinaison (positif : penché en avant), roulis. */
    float twist, lean, roll;
    /** Inclinaison ajoutée de la tête (positif : baissée). */
    float nod;
    /** Bras et jambes : rotations absolues x, y, z. */
    final float[] rArm = new float[3], lArm = new float[3], rLeg = new float[3], lLeg = new float[3];
    /** Les bras visent-ils là où l'on regarde (ajout du tangage de la tête et du lacet du regard) ? 0..1 */
    float rAim, lAim;
    /** Descente du corps (blocs, positif vers le bas) et avancée dans la direction du regard (blocs). */
    float sink, lunge;
    /** Poids de la pose face à la pose normale de Minecraft (0 : rien, 1 : pose seule). */
    float weight = 1f;
    /**
     * Coudes et genoux (radians) pour les personnages GeckoLib, qui en ont : coude négatif = avant-bras relevé vers
     * l'avant, genou positif = jambe pliée vers l'arrière. Le modèle de Minecraft (sans articulation) les ignore.
     */
    float rElbow, lElbow, rKnee, lKnee;

    CombatPose copy() {
        CombatPose p = new CombatPose();
        p.twist = twist; p.lean = lean; p.roll = roll; p.nod = nod;
        System.arraycopy(rArm, 0, p.rArm, 0, 3);
        System.arraycopy(lArm, 0, p.lArm, 0, 3);
        System.arraycopy(rLeg, 0, p.rLeg, 0, 3);
        System.arraycopy(lLeg, 0, p.lLeg, 0, 3);
        p.rAim = rAim; p.lAim = lAim; p.sink = sink; p.lunge = lunge; p.weight = weight;
        p.rElbow = rElbow; p.lElbow = lElbow; p.rKnee = rKnee; p.lKnee = lKnee;
        return p;
    }

    /** Même pose, côté gauche (pour un gaucher, ou le coup de l'autre main). */
    CombatPose mirror() {
        CombatPose p = new CombatPose();
        p.twist = -twist; p.lean = lean; p.roll = -roll; p.nod = nod;
        mirrorLimb(lArm, p.rArm);
        mirrorLimb(rArm, p.lArm);
        mirrorLimb(lLeg, p.rLeg);
        mirrorLimb(rLeg, p.lLeg);
        p.rAim = lAim; p.lAim = rAim; p.sink = sink; p.lunge = lunge; p.weight = weight;
        p.rElbow = lElbow; p.lElbow = rElbow; p.rKnee = lKnee; p.lKnee = rKnee;
        return p;
    }

    private static void mirrorLimb(float[] src, float[] dst) {
        dst[0] = src[0];
        dst[1] = -src[1];
        dst[2] = -src[2];
    }

    static CombatPose lerp(CombatPose a, CombatPose b, float k) {
        CombatPose p = new CombatPose();
        p.twist = Mth.lerp(k, a.twist, b.twist);
        p.lean = Mth.lerp(k, a.lean, b.lean);
        p.roll = Mth.lerp(k, a.roll, b.roll);
        p.nod = Mth.lerp(k, a.nod, b.nod);
        for (int i = 0; i < 3; i++) {
            p.rArm[i] = Mth.lerp(k, a.rArm[i], b.rArm[i]);
            p.lArm[i] = Mth.lerp(k, a.lArm[i], b.lArm[i]);
            p.rLeg[i] = Mth.lerp(k, a.rLeg[i], b.rLeg[i]);
            p.lLeg[i] = Mth.lerp(k, a.lLeg[i], b.lLeg[i]);
        }
        p.rAim = Mth.lerp(k, a.rAim, b.rAim);
        p.lAim = Mth.lerp(k, a.lAim, b.lAim);
        p.sink = Mth.lerp(k, a.sink, b.sink);
        p.lunge = Mth.lerp(k, a.lunge, b.lunge);
        p.weight = Mth.lerp(k, a.weight, b.weight);
        p.rElbow = Mth.lerp(k, a.rElbow, b.rElbow);
        p.lElbow = Mth.lerp(k, a.lElbow, b.lElbow);
        p.rKnee = Mth.lerp(k, a.rKnee, b.rKnee);
        p.lKnee = Mth.lerp(k, a.lKnee, b.lKnee);
        return p;
    }

    // ---------- Construction lisible des images clés ----------

    CombatPose body(float twist, float lean) { this.twist = twist; this.lean = lean; return this; }
    CombatPose roll(float roll) { this.roll = roll; return this; }
    CombatPose nod(float nod) { this.nod = nod; return this; }
    CombatPose rArm(float x, float y, float z) { set(rArm, x, y, z); return this; }
    CombatPose lArm(float x, float y, float z) { set(lArm, x, y, z); return this; }
    CombatPose rLeg(float x, float y, float z) { set(rLeg, x, y, z); return this; }
    CombatPose lLeg(float x, float y, float z) { set(lLeg, x, y, z); return this; }
    CombatPose aim(float right, float left) { rAim = right; lAim = left; return this; }
    CombatPose move(float sink, float lunge) { this.sink = sink; this.lunge = lunge; return this; }
    CombatPose weight(float w) { this.weight = w; return this; }
    CombatPose elbows(float right, float left) { rElbow = right; lElbow = left; return this; }
    CombatPose knees(float right, float left) { rKnee = right; lKnee = left; return this; }

    private static void set(float[] a, float x, float y, float z) {
        a[0] = x; a[1] = y; a[2] = z;
    }

    // ---------- Courbes d'accélération ----------

    enum Ease {
        /** Démarrage et arrivée doux (anticipation, armement). */
        SMOOTH,
        /** Départ explosif, arrivée amortie (l'impact). */
        SNAP,
        /** Retour à la garde, lent puis posé. */
        RECOVER,
        LINEAR;

        float apply(float x) {
            x = Mth.clamp(x, 0f, 1f);
            return switch (this) {
                case SMOOTH -> x * x * (3f - 2f * x);
                case SNAP -> 1f - (1f - x) * (1f - x) * (1f - x) * (1f - x);         // easeOutQuart
                case RECOVER -> x < 0.5f ? 4f * x * x * x : 1f - (float) Math.pow(-2f * x + 2f, 3) / 2f; // easeInOutCubic
                case LINEAR -> x;
            };
        }
    }

    /** Image clé : la pose atteinte au temps {@code t} (ticks), avec la courbe qui y mène. */
    record Key(float t, CombatPose pose, Ease ease) {}

    /** Pose à l'instant {@code t} d'une suite d'images clés. */
    static CombatPose sample(Key[] keys, float t) {
        if (t <= keys[0].t()) return keys[0].pose();
        for (int i = 1; i < keys.length; i++) {
            Key k = keys[i];
            if (t <= k.t()) {
                Key prev = keys[i - 1];
                float x = (t - prev.t()) / Math.max(1e-4f, k.t() - prev.t());
                return lerp(prev.pose(), k.pose(), k.ease().apply(x));
            }
        }
        return keys[keys.length - 1].pose();
    }
}
