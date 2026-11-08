package com.evandouard.hxh.fx;

import com.evandouard.hxh.HxHMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

/**
 * Effets visuels du Nen dessinés par le mod (direction artistique : traits noirs, formes dessinées, pas de
 * particules de Minecraft). Le serveur décide de l'effet ; les clients proches le dessinent et le font suivre
 * les entités concernées (une chaîne reste accrochée à la cible qui bouge).
 */
public final class NenFx {
    /** Les sortes d'effets. */
    public enum Kind {
        /** Étoile d'impact manga au point visé. */
        IMPACT,
        /** Croissant de coupure (Ciseaux, coups tranchants). */
        SLASH,
        /** Anneau qui s'élargit au sol. */
        SHOCKWAVE,
        /** Boule d'aura qui file du lanceur vers la cible (ou le point visé), puis impact. */
        ORB,
        /** Éclair dentelé du lanceur à la cible. */
        BOLT,
        /** Arcs électriques autour du corps pendant la durée (Godspeed). */
        CRACKLE,
        /** Chaîne de la main du lanceur jusqu'à la cible, qui s'enroule autour d'elle (Chain Jail). */
        CHAIN,
        /** Chaîne à croix enroulée autour de la main du lanceur (Holy Chain). */
        HOLY_CHAIN,
        /** Aiguille lancée qui reste plantée dans la cible. */
        NEEDLE,
        /** Montée d'aura : anneau qui remonte le long du corps et lueur (renforcements, transformations). */
        FLARE,
        /** Boule d'aura qui grossit à côté du lanceur pendant une charge (Pierre de Gon). */
        CHARGE_ORB,
        /** Efface les effets en cours du lanceur (fin de charge). */
        CLEAR,
        /** Les blocs du sol sautent en vague autour du point (coup lourd). Taille = rayon. */
        QUAKE,
        /** Image d'impact plein écran (éclair, lignes de vitesse) pour les joueurs proches de la cible. */
        IMPACT_FRAME
    }

    /**
     * Données d'un effet : sorte, entité source et cible (-1 = aucune), point visé, taille, couleur ARGB, durée.
     */
    public record Payload(int kind, int source, int target, double x, double y, double z, float size, int color,
                          int duration) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(HxHMod.id("nen_fx"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.kind);
                    buf.writeVarInt(p.source);
                    buf.writeVarInt(p.target);
                    buf.writeDouble(p.x);
                    buf.writeDouble(p.y);
                    buf.writeDouble(p.z);
                    buf.writeFloat(p.size);
                    buf.writeInt(p.color);
                    buf.writeVarInt(p.duration);
                },
                buf -> new Payload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readDouble(),
                        buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readInt(), buf.readVarInt()));

        public Kind kindEnum() {
            Kind[] all = Kind.values();
            return kind >= 0 && kind < all.length ? all[kind] : Kind.IMPACT;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Payload payload, IPayloadContext context) {
            context.enqueueWork(() -> clientHandler.accept(payload));
        }
    }

    /** Rempli par le client au démarrage. */
    public static Consumer<Payload> clientHandler = p -> {};

    /** Distance à laquelle les joueurs voient les effets. */
    public static final double VIEW_RANGE = 64;

    private NenFx() {}

    // ---------- Couleurs (aura par type) ----------
    public static final int ENHANCER = 0xFFFF8A3C, TRANSMUTER = 0xFF7FD8FF, EMITTER = 0xFFFF5FA8,
            CONJURER = 0xFFC9D4E6, MANIPULATOR = 0xFFB06BFF, SPECIALIST = 0xFF8A3CFF,
            WHITE = 0xFFFFFFFF, GOLD = 0xFFFFD23F, HEAL = 0xFF52D273, DARK = 0xFF5A2A7A;

    /** Envoie l'effet aux joueurs proches. */
    public static void send(ServerLevel level, Kind kind, Entity source, Entity target, Vec3 at, float size, int color,
                            int duration) {
        Payload p = new Payload(kind.ordinal(), source == null ? -1 : source.getId(), target == null ? -1 : target.getId(),
                at.x, at.y, at.z, size, color, duration);
        // Seulement aux joueurs proches qui ont le mod (pas aux faux joueurs des tests)
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) <= VIEW_RANGE * VIEW_RANGE) com.evandouard.hxh.network.HxHNetwork.sendTo(player, p);
        }
    }

    // ---------- Raccourcis ----------

    public static void impactAt(ServerLevel level, Vec3 at, float size, int color) {
        send(level, Kind.IMPACT, null, null, at, size, color, 9);
    }

    public static void slashAt(ServerLevel level, Vec3 at, float size, int color) {
        send(level, Kind.SLASH, null, null, at, size, color, 7);
    }

    public static void shockwaveAt(ServerLevel level, Vec3 at, float radius, int color) {
        send(level, Kind.SHOCKWAVE, null, null, at, radius, color, 14);
    }

    /** Boule de la Pierre à côté du lanceur pendant la charge (jusqu'à {@code maxTicks}). */
    public static void chargeOrb(LivingEntity caster, int color, int maxTicks, boolean epic) {
        // taille : 1 = Pierre normale (demi-joueur, couleurs), 2 = sans retenue (enfle puis se recondense, aveuglante)
        if (caster.level() instanceof ServerLevel s) {
            send(s, Kind.CHARGE_ORB, caster, null, caster.position(), epic ? 2f : 1f, color, maxTicks);
        }
    }

    /** Efface la boule en charge du lanceur (relâchée ou annulée). */
    public static void clear(LivingEntity caster) {
        if (caster.level() instanceof ServerLevel s) send(s, Kind.CLEAR, caster, null, caster.position(), 1f, 0, 1);
    }

    public static void impact(LivingEntity source, Vec3 at, float size, int color) {
        if (source.level() instanceof ServerLevel s) send(s, Kind.IMPACT, source, null, at, size, color, 8);
    }

    public static void impactOn(LivingEntity source, LivingEntity target, float size, int color) {
        impact(source, target.getBoundingBox().getCenter(), size, color);
    }

    public static void slash(LivingEntity source, Vec3 at, float size, int color) {
        if (source.level() instanceof ServerLevel s) send(s, Kind.SLASH, source, null, at, size, color, 7);
    }

    public static void shockwave(LivingEntity source, float radius, int color) {
        if (source.level() instanceof ServerLevel s) send(s, Kind.SHOCKWAVE, source, null, source.position(), radius, color, 12);
    }

    /** Boule d'aura vers la cible (ou vers {@code at} si pas de cible). */
    public static void orb(LivingEntity source, LivingEntity target, Vec3 at, float size, int color) {
        if (source.level() instanceof ServerLevel s) send(s, Kind.ORB, source, target, at, size, color, 30);
    }

    public static void bolt(LivingEntity source, LivingEntity target, Vec3 at, int color) {
        if (source.level() instanceof ServerLevel s) send(s, Kind.BOLT, source, target, at, 1f, color, 8);
    }

    public static void crackle(LivingEntity entity, int color, int duration) {
        if (entity.level() instanceof ServerLevel s) send(s, Kind.CRACKLE, entity, null, entity.position(), 1f, color, duration);
    }

    public static void chain(LivingEntity source, LivingEntity target, Vec3 at, int color, int duration) {
        if (source.level() instanceof ServerLevel s) send(s, Kind.CHAIN, source, target, at, 1f, color, duration);
    }

    public static void holyChain(LivingEntity source, int color, int duration) {
        if (source.level() instanceof ServerLevel s) send(s, Kind.HOLY_CHAIN, source, null, source.position(), 1f, color, duration);
    }

    public static void needle(LivingEntity source, LivingEntity target, Vec3 at, int duration) {
        if (source.level() instanceof ServerLevel s) send(s, Kind.NEEDLE, source, target, at, 1f, WHITE, duration);
    }

    public static void quake(ServerLevel level, Vec3 at, float radius) {
        send(level, Kind.QUAKE, null, null, at, radius, WHITE, (int) (radius * 2 + 10));
    }

    /** Image d'impact : vue par les joueurs à moins de 24 blocs de la cible. */
    public static void impactFrame(LivingEntity source, LivingEntity target, int duration) {
        if (source.level() instanceof ServerLevel s) {
            send(s, Kind.IMPACT_FRAME, source, target, target.getBoundingBox().getCenter(), 1f, WHITE, duration);
        }
    }

    public static void flare(LivingEntity entity, float size, int color) {
        if (entity.level() instanceof ServerLevel s) send(s, Kind.FLARE, entity, null, entity.position(), size, color, 16);
    }
}
