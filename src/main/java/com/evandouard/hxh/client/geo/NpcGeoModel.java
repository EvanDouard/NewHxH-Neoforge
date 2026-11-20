package com.evandouard.hxh.client.geo;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.client.look.CharacterLooks;
import com.evandouard.hxh.npc.NpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modèle GeckoLib d'un personnage, choisi par l'identifiant du PNJ :
 * {@code geo/entity/npc/<id>.geo.json}, {@code animations/entity/npc/<id>.animation.json},
 * {@code textures/entity/npc_geo/<id>.png} (générés par tools/models). La tête suit le regard (os {@code head}).
 */
public class NpcGeoModel extends GeoModel<NpcEntity> {
    private static final Map<String, Boolean> EXISTS = new ConcurrentHashMap<>();

    static {
        CharacterLooks.onReload(EXISTS::clear);
    }

    /** Le personnage a-t-il un modèle GeckoLib ? Sinon on garde le rendu provisoire (modèle du joueur). */
    public static boolean has(String id) {
        if (id == null || id.isEmpty()) return false;
        return EXISTS.computeIfAbsent(id, i ->
                Minecraft.getInstance().getResourceManager().getResource(geo(i)).isPresent());
    }

    static ResourceLocation geo(String id) {
        return HxHMod.id("geo/entity/npc/" + id + ".geo.json");
    }

    @Override
    public ResourceLocation getModelResource(NpcEntity npc) {
        return geo(npc.npcId());
    }

    @Override
    public ResourceLocation getTextureResource(NpcEntity npc) {
        return HxHMod.id("textures/entity/npc_geo/" + npc.npcId() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(NpcEntity npc) {
        return HxHMod.id("animations/entity/npc/" + npc.npcId() + ".animation.json");
    }

    @Override
    public void setCustomAnimations(NpcEntity npc, long instanceId, AnimationState<NpcEntity> state) {
        GeoBone head = getAnimationProcessor().getBone("head");
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head != null && data != null) {
            head.setRotX(data.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(data.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }
}
