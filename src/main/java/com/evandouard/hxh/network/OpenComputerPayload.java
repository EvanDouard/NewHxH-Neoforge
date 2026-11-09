package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Serveur → client : les fiches de l'ordinateur Hunter vues par ce joueur (texte vide si verrouillée). */
public record OpenComputerPayload(boolean hunter, List<Entry> entries) implements CustomPacketPayload {
    public record Entry(String id, String category, String title, String text, boolean unlocked, String hint) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::id,
                ByteBufCodecs.STRING_UTF8, Entry::category,
                ByteBufCodecs.STRING_UTF8, Entry::title,
                ByteBufCodecs.stringUtf8(32767), Entry::text,
                ByteBufCodecs.BOOL, Entry::unlocked,
                ByteBufCodecs.STRING_UTF8, Entry::hint,
                Entry::new);
    }

    public static final Type<OpenComputerPayload> TYPE = new Type<>(HxHMod.id("open_computer"));
    public static final StreamCodec<ByteBuf, OpenComputerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OpenComputerPayload::hunter,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenComputerPayload::entries,
            OpenComputerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenComputerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.evandouard.hxh.client.ClientHooks.openComputer(payload));
    }
}
