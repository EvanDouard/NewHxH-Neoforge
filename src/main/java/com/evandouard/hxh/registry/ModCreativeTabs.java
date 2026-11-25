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
                        output.accept(ModItems.GON_SPAWN_EGG.get());
                        output.accept(ModItems.GON_ULTIME_SPAWN_EGG.get());
                        output.accept(ModItems.KILLUA_SPAWN_EGG.get());
                        output.accept(ModItems.KURAPIKA_SPAWN_EGG.get());
                        output.accept(ModItems.KURAPIKA_SANS_TUNIQUE_SPAWN_EGG.get());
                        output.accept(ModItems.LEORIO_SPAWN_EGG.get());
                        output.accept(ModItems.HISOKA_SPAWN_EGG.get());
                        output.accept(ModItems.KNUCKLE_SPAWN_EGG.get());
                        output.accept(ModItems.PHINKS_SPAWN_EGG.get());
                        output.accept(ModItems.GING_SPAWN_EGG.get());
                        output.accept(ModItems.PAKUNODA_SPAWN_EGG.get());
                        output.accept(ModItems.SHALNARK_SPAWN_EGG.get());
                        output.accept(ModItems.KALLUTO_SPAWN_EGG.get());
                        output.accept(ModItems.KITE_SPAWN_EGG.get());
                        output.accept(ModItems.SILVA_SPAWN_EGG.get());
                        output.accept(ModItems.MACHI_SPAWN_EGG.get());
                        output.accept(ModItems.NOBUNAGA_SPAWN_EGG.get());
                        output.accept(ModItems.FEITAN_SPAWN_EGG.get());
                        output.accept(ModItems.BISCUIT_SPAWN_EGG.get());
                        output.accept(ModItems.ZENO_SPAWN_EGG.get());
                        output.accept(ModItems.ILLUMI_SPAWN_EGG.get());
                        output.accept(ModItems.UVOGIN_SPAWN_EGG.get());
                        output.accept(ModItems.CHROLLO_SPAWN_EGG.get());
                        output.accept(ModItems.YOUPI_SPAWN_EGG.get());
                        output.accept(ModItems.SHAIAPOUF_SPAWN_EGG.get());
                        output.accept(ModItems.NEFERPITOU_SPAWN_EGG.get());
                        output.accept(ModItems.MERUEM_SPAWN_EGG.get());
                        output.accept(ModItems.NETERO_SPAWN_EGG.get());
                        output.accept(ModItems.FOXBEAR_SPAWN_EGG.get());
                        output.accept(ModItems.SPIDER_EAGLE_SPAWN_EGG.get());
                        output.accept(ModItems.MAN_FACED_APE_SPAWN_EGG.get());
                        output.accept(ModItems.CHIMERA_ANT_SPAWN_EGG.get());
                        output.accept(ModItems.KIRIKO_SPAWN_EGG.get());
                        output.accept(ModItems.GREAT_STAMP_SPAWN_EGG.get());
                        output.accept(ModItems.RAW_FOXBEAR_MEAT.get());
                        output.accept(ModItems.COOKED_FOXBEAR_MEAT.get());
                        output.accept(ModItems.GOURMET_RECIPE.get());
                        output.accept(ModItems.WATER_DIVINATION.get());
                        output.accept(ModItems.HUNTER_COMPUTER.get());
                        output.accept(ModItems.AIRSHIP_TERMINAL.get());
                        output.accept(ModItems.HEAVY_DOOR.get());
                        output.accept(ModItems.EXAM_BADGE.get());
                        output.accept(ModItems.EXAM_PLATE.get());
                        output.accept(ModItems.TARGET_PLATE.get());
                        output.accept(ModItems.SPIDER_EAGLE_EGG.get());
                        output.accept(ModItems.NETERO_BALL.get());
                        output.accept(ModItems.SUNKEN_COIN.get());
                        ModItems.DUMBBELLS.forEach(i -> output.accept(i.get()));
                        ModItems.DISHES.forEach(i -> output.accept(i.get()));
                        ModItems.TREADMILLS.forEach(i -> output.accept(i.get()));
                        output.accept(ModItems.STRIKING_DUMMY.get());
                        output.accept(ModItems.ICE_BATH.get());
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
                        output.accept(ModItems.ILLUSION_WALL.get());
                        ModItems.MEDITATION_STONES.forEach(i -> output.accept(i.get()));
                        output.accept(ModItems.MARKER_ARRIVAL.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
