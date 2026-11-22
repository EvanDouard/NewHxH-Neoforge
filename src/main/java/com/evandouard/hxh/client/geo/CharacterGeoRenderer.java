package com.evandouard.hxh.client.geo;

import com.evandouard.hxh.mob.CharacterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Rendu GeckoLib des personnages en mob (Gon, Killua…). */
public class CharacterGeoRenderer extends GeoEntityRenderer<CharacterEntity> {
    public CharacterGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new CharacterGeoModel());
        this.shadowRadius = 0.45f;
    }
}
