package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Serveur → client : quête suivie à afficher (titre vide = aucune). Les textes arrivent déjà prêts
 * (clé de traduction avec, au besoin, le texte écrit dans le JSON de la quête comme repli).
 */
public record QuestStatusPayload(Component title, Component step, int progress, int goal, long target) implements CustomPacketPayload {
    /** Pas de cible pour la boussole. */
    public static final long NO_TARGET = Long.MIN_VALUE;

    public QuestStatusPayload(Component title, Component step, int progress, int goal) {
        this(title, step, progress, goal, NO_TARGET);
    }

    /** Copie avec la position que la boussole doit pointer (BlockPos.asLong, ou NO_TARGET). */
    public QuestStatusPayload withTarget(long pos) {
        return new QuestStatusPayload(title, step, progress, goal, pos);
    }

    /**
     * Angle à l'écran (radians, sens horaire depuis le haut) de la direction (dx, dz) vue par quelqu'un
     * qui regarde vers {@code yawDeg} (lacet de Minecraft : 0 = sud/+Z, 90 = ouest/-X).
     */
    public static float compassAngle(float yawDeg, double dx, double dz) {
        double yaw = Math.toRadians(yawDeg);
        double fwd = dx * -Math.sin(yaw) + dz * Math.cos(yaw);
        double right = dx * -Math.cos(yaw) + dz * -Math.sin(yaw);
        return (float) Math.atan2(right, fwd);
    }

    public boolean hasTarget() {
        return target != NO_TARGET;
    }

    public static final Type<QuestStatusPayload> TYPE = new Type<>(HxHMod.id("quest_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.TRUSTED_STREAM_CODEC, QuestStatusPayload::title,
            ComponentSerialization.TRUSTED_STREAM_CODEC, QuestStatusPayload::step,
            ByteBufCodecs.VAR_INT, QuestStatusPayload::progress,
            ByteBufCodecs.VAR_INT, QuestStatusPayload::goal,
            ByteBufCodecs.VAR_LONG, QuestStatusPayload::target,
            QuestStatusPayload::new);

    public static final QuestStatusPayload NONE = new QuestStatusPayload(Component.empty(), Component.empty(), 0, 0);

    /** Dernier état reçu (lu par l'interface en jeu). */
    public static volatile QuestStatusPayload latest = NONE;

    public boolean isEmpty() {
        return title.getString().isEmpty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> latest = payload);
    }
}
