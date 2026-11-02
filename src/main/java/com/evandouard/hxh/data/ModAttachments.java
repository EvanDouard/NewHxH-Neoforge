package com.evandouard.hxh.data;

import com.evandouard.hxh.HxHMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HxHMod.MODID);

    /** Données Hunter. Copiées à la mort du joueur (on garde ses stats). */
    public static final Supplier<AttachmentType<HunterData>> HUNTER = TYPES.register("hunter",
            () -> AttachmentType.serializable(HunterData::new).copyOnDeath().build());

    private ModAttachments() {}
}
