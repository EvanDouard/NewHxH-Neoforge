package com.evandouard.hxh;

import com.evandouard.hxh.registry.ModCreativeTabs;
import com.evandouard.hxh.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Point d'entrée commun (client + serveur) du mod.
 */
@Mod(HxHMod.MODID)
public final class HxHMod {
    public static final String MODID = "hxh";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HxHMod(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        LOGGER.info("[HxH] Mod chargé");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
