package com.evandouard.hxh.client.geo;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.summon.NenSummonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Modèles des invocations : {@code summon_<modèle>} (générés par tools/models/summon_*.py). */
public class SummonGeoModel extends GeoModel<NenSummonEntity> {
    private static String id(NenSummonEntity e) {
        return "summon_" + e.model();
    }

    @Override
    public ResourceLocation getModelResource(NenSummonEntity e) {
        return NpcGeoModel.geo(id(e));
    }

    @Override
    public ResourceLocation getTextureResource(NenSummonEntity e) {
        return NpcGeoModel.texture(id(e));
    }

    @Override
    public ResourceLocation getAnimationResource(NenSummonEntity e) {
        return HxHMod.id("animations/entity/npc/" + id(e) + ".animation.json");
    }
}
