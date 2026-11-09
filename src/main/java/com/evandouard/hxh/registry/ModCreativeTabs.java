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
                        output.accept(ModItems.HUNTER_COMPUTER.get());
                        output.accept(ModItems.EXAM_BADGE.get());
                        ModItems.DUMBBELLS.forEach(i -> output.accept(i.get()));
                        ModItems.TREADMILLS.forEach(i -> output.accept(i.get()));
                        output.accept(ModItems.HEAVENS_ARENA_GATE.get());
                        output.accept(ModItems.ARENA_ELEVATOR.get());
                        output.accept(ModItems.ARENA_EXIT.get());
                        output.accept(ModItems.MARKER_PLAYER.get());
                        output.accept(ModItems.MARKER_OPPONENT.get());
                        output.accept(ModItems.MARKER_RECEPTIONIST.get());
                        output.accept(ModItems.MARKER_REFEREE.get());
                        output.accept(ModItems.MARKER_NPC_CHOICE.get());
                        output.accept(ModItems.MARKER_WAYPOINT.get());
                        output.accept(ModItems.MARKER_PREY.get());
                        output.accept(ModItems.MARKER_ARRIVAL.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
