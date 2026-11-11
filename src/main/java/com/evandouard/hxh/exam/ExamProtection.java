package com.evandouard.hxh.exam;

import com.evandouard.hxh.HxHMod;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Le monde de l'Examen est un décor : on n'y casse ni n'y pose de bloc (sauf en créatif, pour construire),
 * les explosions n'abîment rien, et aucun mob de Minecraft n'y apparaît tout seul (seuls ceux de l'Examen).
 */
public final class ExamProtection {
    private ExamProtection() {}

    public static void register(IEventBus bus) {
        bus.addListener(BlockEvent.BreakEvent.class, e -> {
            if (ExamWorld.isExamLevel(e.getPlayer().level()) && !e.getPlayer().isCreative()) e.setCanceled(true);
        });
        bus.addListener(BlockEvent.EntityPlaceEvent.class, e -> {
            if (!(e.getLevel() instanceof net.minecraft.world.level.Level l) || !ExamWorld.isExamLevel(l)) return;
            if (e.getEntity() instanceof Player p && p.isCreative()) return;
            e.setCanceled(true);
        });
        bus.addListener(ExplosionEvent.Detonate.class, e -> {
            if (ExamWorld.isExamLevel(e.getLevel())) e.getAffectedBlocks().clear();
        });
        bus.addListener(PlayerInteractEvent.RightClickItem.class, e -> {
            if (forbidden(e.getEntity(), e.getItemStack().getItem())) e.setCanceled(true);
        });
        bus.addListener(PlayerInteractEvent.RightClickBlock.class, e -> {
            if (forbidden(e.getEntity(), e.getItemStack().getItem())) e.setCanceled(true);
        });
        bus.addListener(FinalizeSpawnEvent.class, e -> {
            if (!ExamWorld.isExamLevel(e.getEntity().level())) return;
            MobSpawnType t = e.getSpawnType();
            if (t == MobSpawnType.NATURAL || t == MobSpawnType.CHUNK_GENERATION || t == MobSpawnType.PATROL
                    || t == MobSpawnType.REINFORCEMENT || t == MobSpawnType.JOCKEY) {
                e.setSpawnCancelled(true);
            }
        });
        HxHMod.LOGGER.debug("[HxH] Protections de l'Examen actives");
    }

    private static boolean forbidden(Player player, Item item) {
        if (!ExamWorld.isExamLevel(player.level()) || player.isCreative()) return false;
        return item instanceof BucketItem || item instanceof FlintAndSteelItem || item instanceof FireChargeItem;
    }
}
