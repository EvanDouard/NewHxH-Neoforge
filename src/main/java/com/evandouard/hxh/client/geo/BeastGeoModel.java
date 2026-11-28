package com.evandouard.hxh.client.geo;

import com.evandouard.hxh.HxHMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Modèle d'une bête du bestiaire : {@code beast_<type d'entité>} (géo, animations et texture générées par
 * tools/models/beast_*.py, rangées avec celles des personnages). La tête suit le regard (os {@code head}).
 */
public class BeastGeoModel<T extends LivingEntity & GeoAnimatable> extends GeoModel<T> {
    static String id(LivingEntity e) {
        return "beast_" + BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
    }

    @Override
    public ResourceLocation getModelResource(T e) {
        return NpcGeoModel.geo(id(e));
    }

    @Override
    public ResourceLocation getTextureResource(T e) {
        return NpcGeoModel.texture(id(e));
    }

    @Override
    public ResourceLocation getAnimationResource(T e) {
        return HxHMod.id("animations/entity/npc/" + id(e) + ".animation.json");
    }

    @Override
    public void setCustomAnimations(T e, long instanceId, AnimationState<T> state) {
        GeoBone head = getAnimationProcessor().getBone("head");
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head != null && data != null) {
            head.setRotX(head.getRotX() + data.headPitch() * Mth.DEG_TO_RAD * 0.6f);
            head.setRotY(head.getRotY() + Mth.clamp(data.netHeadYaw(), -50f, 50f) * Mth.DEG_TO_RAD);
        }
    }
}
