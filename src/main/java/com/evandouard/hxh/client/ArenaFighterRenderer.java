package com.evandouard.hxh.client;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.arena.ArenaFighterEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Combattants de la Tour Céleste : modèle du joueur et quelques skins de lutteurs (provisoire). */
public class ArenaFighterRenderer extends HumanoidMobRenderer<ArenaFighterEntity, PlayerModel<ArenaFighterEntity>> {
    public ArenaFighterRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ArenaFighterEntity entity) {
        return HxHMod.id("textures/entity/arena_fighter/fighter_" + Math.floorMod(entity.skin(), ArenaFighterEntity.SKINS) + ".png");
    }
}
