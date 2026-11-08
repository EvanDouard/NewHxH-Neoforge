package com.evandouard.hxh.client;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.npc.NpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** PNJ dessinés avec le modèle du joueur et un skin par personnage (provisoire en attendant les modèles). */
public class NpcRenderer extends HumanoidMobRenderer<NpcEntity, PlayerModel<NpcEntity>> {
    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity) {
        return SKINS.computeIfAbsent(entity.npcId(), id -> {
            ResourceLocation own = HxHMod.id("textures/entity/npc/" + id + ".png");
            // PNJ créé dans une quête sans skin : apparence par défaut (ajouter textures/entity/npc/<id>.png)
            return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(own).isPresent()
                    ? own : DEFAULT;
        });
    }

    private static final ResourceLocation DEFAULT = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    private static final java.util.Map<String, ResourceLocation> SKINS = new java.util.concurrent.ConcurrentHashMap<>();
}
