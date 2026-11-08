package com.evandouard.hxh.mob;

import com.evandouard.hxh.HxHMod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/** Charge les mobs d'exemple ({@code mob_presets}) ; rechargés par /reload. */
public final class MobPresets extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, MobPreset> PRESETS = Map.of();

    private MobPresets() {
        super(new Gson(), "mob_presets");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new MobPresets());
    }

    /** Comportement des mannequins d'entraînement. */
    public static void registerEvents(net.neoforged.bus.api.IEventBus bus) {
        // Ne meurt jamais (sauf /kill) : on voit les dégâts, puis il revient à fond
        bus.addListener(net.neoforged.neoforge.event.entity.living.LivingDeathEvent.class, e -> {
            net.minecraft.world.entity.LivingEntity l = e.getEntity();
            if (l.getTags().contains(MobPreset.DUMMY_TAG)
                    && !e.getSource().is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) {
                e.setCanceled(true);
                l.setHealth(l.getMaxHealth());
            }
        });
        // Très forte régénération : 10 % de sa vie par seconde
        bus.addListener(net.neoforged.neoforge.event.tick.EntityTickEvent.Post.class, e -> {
            if (e.getEntity() instanceof net.minecraft.world.entity.LivingEntity l && !l.level().isClientSide()
                    && l.tickCount % 10 == 0 && l.getTags().contains(MobPreset.DUMMY_TAG) && l.getHealth() < l.getMaxHealth()) {
                l.heal(l.getMaxHealth() * 0.05f);
            }
        });
        // Accroupi + clic droit main vide : on le range
        bus.addListener(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract.class, e -> {
            if (e.getTarget().getTags().contains(MobPreset.DUMMY_TAG) && e.getEntity().isShiftKeyDown()
                    && e.getItemStack().isEmpty() && e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
                if (!e.getLevel().isClientSide()) {
                    e.getTarget().discard();
                    e.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.hxh.dummy_removed"), true);
                }
                e.setCanceled(true);
                e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }
        });
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, MobPreset> map = new TreeMap<>();
        files.forEach((id, json) -> {
            try {
                map.put(id, MobPreset.parse(id, json));
            } catch (RuntimeException e) {
                HxHMod.LOGGER.error("[HxH] Mob d'exemple invalide {}", id, e);
            }
        });
        PRESETS = Map.copyOf(map);
        HxHMod.LOGGER.info("[HxH] {} mob(s) d'exemple chargé(s)", map.size());
    }

    public static Map<ResourceLocation, MobPreset> all() {
        return new TreeMap<>(PRESETS);
    }

    /** Par identifiant complet, ou par nom seul ({@code zombie_costaud} = {@code hxh:zombie_costaud}). */
    @Nullable
    public static MobPreset get(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name.contains(":") ? name : HxHMod.MODID + ":" + name);
        return id == null ? null : PRESETS.get(id);
    }
}
