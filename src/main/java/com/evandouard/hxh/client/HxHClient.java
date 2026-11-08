package com.evandouard.hxh.client;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.client.ci.CiAutopilot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Point d'entrée côté client uniquement.
 */
@Mod(value = HxHMod.MODID, dist = Dist.CLIENT)
public final class HxHClient {
    public HxHClient(IEventBus modEventBus, ModContainer container) {
        NenClient.register(modEventBus);
        LevelTagRenderer.register();
        DamageNumberRenderer.register();
        CombatAnimClient.register();
        FearClient.register();
        NenFxClient.register();
        CiAutopilot.initIfRequested();
    }
}
