package com.evandouard.hxh.registry;

import com.evandouard.hxh.HxHMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HxHMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hxh"))
                    .icon(() -> ModItems.HUNTER_CARD.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.HUNTER_CARD.get());
                        output.accept(ModItems.WATER_DIVINATION.get());
                        output.accept(ModItems.EXAM_BADGE.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
