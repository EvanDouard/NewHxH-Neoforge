package com.evandouard.hxh;

import com.evandouard.hxh.command.HunterCommand;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.network.HxHNetwork;
import com.evandouard.hxh.registry.ModBlocks;
import com.evandouard.hxh.registry.ModCreativeTabs;
import com.evandouard.hxh.registry.ModItems;
import com.evandouard.hxh.stats.HunterEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Point d'entrée commun (client + serveur) du mod.
 */
@Mod(HxHMod.MODID)
public final class HxHMod {
    public static final String MODID = "hxh";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HxHMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        com.evandouard.hxh.registry.ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModAttachments.TYPES.register(modEventBus);
        com.evandouard.hxh.effect.ModEffects.EFFECTS.register(modEventBus);
        com.evandouard.hxh.registry.ModEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(com.evandouard.hxh.registry.ModEntities::onAttributes);
        NeoForge.EVENT_BUS.addListener(com.evandouard.hxh.quest.QuestRegistry::register);
        NeoForge.EVENT_BUS.addListener(com.evandouard.hxh.arena.ArenaBrackets::register);
        NeoForge.EVENT_BUS.addListener(com.evandouard.hxh.mob.MobPresets::register);
        NeoForge.EVENT_BUS.addListener(com.evandouard.hxh.shop.ShopRegistry::register);
        NeoForge.EVENT_BUS.addListener(com.evandouard.hxh.computer.HunterComputer::register);
        NeoForge.EVENT_BUS.addListener(com.evandouard.hxh.exam.ExamPlacer::onServerStarted);
        com.evandouard.hxh.mob.MobPresets.registerEvents(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.ServerTickEvent.Post.class,
                e -> com.evandouard.hxh.hatsu.NenCharge.tickAll());
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.ServerTickEvent.Post.class,
                e -> com.evandouard.hxh.combat.Impacts.tickAll());
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.ServerTickEvent.Post.class,
                e -> com.evandouard.hxh.hatsu.LingeringNen.tickAll());
        // Tour Céleste : posée par le mod dans une plaine, une fois par monde
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.server.ServerStartedEvent.class,
                e -> com.evandouard.hxh.arena.TowerPlacer.ensurePlaced(e.getServer()));
        com.evandouard.hxh.quest.QuestEvents.register(NeoForge.EVENT_BUS);
        com.evandouard.hxh.exam.ExamTournament.register(NeoForge.EVENT_BUS);
        com.evandouard.hxh.exam.Exam288.register(NeoForge.EVENT_BUS);
        com.evandouard.hxh.arena.ArenaManager.register(NeoForge.EVENT_BUS);
        com.evandouard.hxh.training.TrainingEvents.register(NeoForge.EVENT_BUS);
        modEventBus.addListener(HxHNetwork::register);
        modContainer.registerConfig(ModConfig.Type.SERVER, HxHConfig.SPEC);
        HxHGameRules.init();

        HunterEvents.register(NeoForge.EVENT_BUS);

        NeoForge.EVENT_BUS.addListener(HunterCommand::register);
        LOGGER.info("[HxH] Mod chargé");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
