package com.evandouard.hxh.nen;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Formes de l'En personnalisé (à partir de la maîtrise {@link #UNLOCK_LEVEL}) : sphère (forme de base), demi-sphère,
 * cube (rayon + hauteur) et cylindre (rayon, hauteur au-dessus, hauteur en dessous). Toutes gardent le volume de la
 * sphère de base de rayon R ({@link NenSenses#enRadius}) : on choisit une ou deux dimensions, la dernière est
 * calculée (arrondie au dixième). L'origine est toujours l'utilisateur (ses pieds ; le milieu du corps pour la sphère).
 */
public enum EnShape {
    SPHERE("sphere"), HALF_SPHERE("half_sphere"), CUBE("cube"), CYLINDER("cylinder");

    /** Maîtrise de l'En qui débloque les formes. */
    public static final int UNLOCK_LEVEL = 50;
    /** Plus petite dimension permise, et plus grande (hauteur ou rayon). */
    public static final double MIN_SIZE = 1, MAX_SIZE = 64;

    private final String id;

    EnShape(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "en_shape.hxh." + id;
    }

    /** Le cylindre se règle avec deux valeurs (rayon et hauteur au-dessus), les autres formes avec une. */
    public boolean twoParams() {
        return this == CYLINDER;
    }

    /** La première valeur se règle (cube, cylindre) ; sphère et demi-sphère se déduisent entièrement de R. */
    public boolean adjustable() {
        return this == CUBE || this == CYLINDER;
    }

    public EnShape next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static EnShape byId(String id) {
        for (EnShape s : values()) if (s.id.equals(id)) return s;
        return SPHERE;
    }

    /** Dimensions résolues : rayon (demi-côté pour le cube), hauteur au-dessus et en dessous de l'origine. */
    public record Dims(EnShape shape, double radius, double up, double down) {
        public Dims scaled(double k) {
            return new Dims(shape, radius * k, up * k, down * k);
        }

        public double volume() {
            return switch (shape) {
                case SPHERE -> 4.0 / 3.0 * Math.PI * radius * radius * radius;
                case HALF_SPHERE -> 2.0 / 3.0 * Math.PI * radius * radius * radius;
                case CUBE -> 4 * radius * radius * (up + down);
                case CYLINDER -> Math.PI * radius * radius * (up + down);
            };
        }
    }

    /** Volume de référence : la sphère de base de rayon {@code baseRadius}. */
    public static double volume(double baseRadius) {
        return 4.0 / 3.0 * Math.PI * baseRadius * baseRadius * baseRadius;
    }

    static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    /** Dimensions de la forme pour un En de rayon de base R et les valeurs choisies {@code a} (rayon) et {@code b}. */
    public static Dims resolve(EnShape shape, double baseRadius, double a, double b) {
        double v = volume(baseRadius);
        return switch (shape) {
            case SPHERE -> new Dims(SPHERE, baseRadius, baseRadius, baseRadius);
            case HALF_SPHERE -> {
                double r = round1(baseRadius * Math.cbrt(2));
                yield new Dims(HALF_SPHERE, r, r, 0);
            }
            case CUBE -> new Dims(CUBE, a, round1(v / (4 * a * a)), 0);
            case CYLINDER -> new Dims(CYLINDER, a, b, round1(v / (Math.PI * a * a) - b));
        };
    }

    /**
     * Réglage par défaut d'une forme pour un En de rayon de base R : cube aussi haut que large, cylindre aussi haut que
     * large, coupé à mi-hauteur. Toujours faisable dès la maîtrise {@link #UNLOCK_LEVEL} (et jusqu'à 100).
     * @return {a, b}
     */
    public static float[] defaults(EnShape shape, double baseRadius) {
        double v = volume(baseRadius);
        return switch (shape) {
            case CUBE -> new float[]{(float) round1(Math.cbrt(v / 8)), 1f};
            case CYLINDER -> new float[]{(float) round1(baseRadius), (float) round1(baseRadius * 2 / 3)};
            default -> new float[]{3f, 1f};
        };
    }

    /** @return null si la forme est faisable, sinon la clé du refus */
    @Nullable
    public static String refusal(EnShape shape, double baseRadius, double a, double b) {
        if (shape == SPHERE || shape == HALF_SPHERE) return null;
        if (Double.isNaN(a) || Double.isNaN(b) || a < MIN_SIZE) return "message.hxh.en_shape_radius_small";
        if (a > MAX_SIZE) return "message.hxh.en_shape_too_big";
        Dims d = resolve(shape, baseRadius, a, b);
        if (shape == CUBE) {
            if (d.up() < MIN_SIZE) return "message.hxh.en_shape_too_flat";
            if (d.up() > MAX_SIZE) return "message.hxh.en_shape_too_big";
            return null;
        }
        if (b < 0) return "message.hxh.en_shape_negative";
        if (d.down() < 0) return "message.hxh.en_shape_up_too_high";
        if (d.up() + d.down() < MIN_SIZE) return "message.hxh.en_shape_too_flat";
        if (d.up() > MAX_SIZE || d.down() > MAX_SIZE) return "message.hxh.en_shape_too_big";
        return null;
    }

    /**
     * Une partie de la boîte {@code box} est-elle dans la forme ? {@code feet} = pieds de l'utilisateur,
     * {@code center} = milieu de son corps (origine de la sphère).
     */
    public static boolean contains(Dims d, Vec3 feet, Vec3 center, AABB box) {
        switch (d.shape()) {
            case SPHERE -> {
                return distSq(box, center, 0) <= d.radius() * d.radius();
            }
            case HALF_SPHERE -> {
                if (box.maxY < feet.y) return false;
                return distSq(box, feet, 1) <= d.radius() * d.radius();
            }
            case CUBE -> {
                return box.intersects(feet.x - d.radius(), feet.y - d.down(), feet.z - d.radius(),
                        feet.x + d.radius(), feet.y + d.up(), feet.z + d.radius());
            }
            default -> {
                if (box.maxY < feet.y - d.down() || box.minY > feet.y + d.up()) return false;
                double dx = Math.max(box.minX - feet.x, Math.max(0, feet.x - box.maxX));
                double dz = Math.max(box.minZ - feet.z, Math.max(0, feet.z - box.maxZ));
                return dx * dx + dz * dz <= d.radius() * d.radius();
            }
        }
    }

    /** Distance² de {@code p} à la boîte ; {@code aboveOnly} = 1 : on ne compte pas ce qui est sous {@code p}. */
    private static double distSq(AABB box, Vec3 p, int aboveOnly) {
        double dx = Math.max(box.minX - p.x, Math.max(0, p.x - box.maxX));
        double dy = aboveOnly == 1 ? Math.max(box.minY - p.y, 0) : Math.max(box.minY - p.y, Math.max(0, p.y - box.maxY));
        double dz = Math.max(box.minZ - p.z, Math.max(0, p.z - box.maxZ));
        return dx * dx + dy * dy + dz * dz;
    }
}
