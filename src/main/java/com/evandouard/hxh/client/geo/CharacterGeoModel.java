package com.evandouard.hxh.client.geo;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.mob.CharacterEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Modèle des personnages en mob : mêmes fichiers que le PNJ du même nom (voir {@link NpcGeoModel}). En combat,
 * texture d'expression {@code <id>_froid} si elle existe (regard d'assassin de Killua).
 */
public class CharacterGeoModel extends GeoModel<CharacterEntity> {
    @Override
    public ResourceLocation getModelResource(CharacterEntity e) {
        return NpcGeoModel.geo(e.modelId());
    }

    @Override
    public ResourceLocation getTextureResource(CharacterEntity e) {
        if (e.isAggressive()) {
            ResourceLocation cold = NpcGeoModel.texture(e.modelId() + "_froid");
            if (Minecraft.getInstance().getResourceManager().getResource(cold).isPresent()) return cold;
        }
        return NpcGeoModel.texture(e.modelId());
    }

    @Override
    public ResourceLocation getAnimationResource(CharacterEntity e) {
        return HxHMod.id("animations/entity/npc/" + e.modelId() + ".animation.json");
    }

    @Override
    public void setCustomAnimations(CharacterEntity e, long instanceId, AnimationState<CharacterEntity> state) {
        GeoBone head = getAnimationProcessor().getBone("head");
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head != null && data != null) {
            head.setRotX(data.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(data.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }
}
