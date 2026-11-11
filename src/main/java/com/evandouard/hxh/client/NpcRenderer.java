package com.evandouard.hxh.client;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.client.look.CharacterExtrasLayer;
import com.evandouard.hxh.client.look.CharacterLook;
import com.evandouard.hxh.client.look.CharacterLooks;
import com.evandouard.hxh.npc.NpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * PNJ dessinés avec le modèle du joueur, habillé par l'apparence du personnage ({@code assets/hxh/looks/<id>.json}) :
 * skin, bras fins, taille (Buhara immense, Beans minuscule) et cubes en plus (cheveux, barbe, ventre…).
 * Sans apparence, on prend {@code textures/entity/npc/<id>.png}, sinon Steve.
 */
public class NpcRenderer extends HumanoidMobRenderer<NpcEntity, PlayerModel<NpcEntity>> {
    private final PlayerModel<NpcEntity> wide;
    private final PlayerModel<NpcEntity> slim;

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.wide = getModel();
        this.slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new CharacterExtrasLayer<>(this, e -> CharacterLooks.get(e.npcId())));
    }

    @Override
    public void render(NpcEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        CharacterLook look = CharacterLooks.get(entity.npcId());
        this.model = look != null && look.slim() ? slim : wide;
        this.shadowRadius = 0.5f * (look != null ? Math.max(look.scaleX(), look.scaleZ()) : 1f);
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    protected void scale(NpcEntity entity, PoseStack poseStack, float partialTick) {
        CharacterLook look = CharacterLooks.get(entity.npcId());
        if (look != null) poseStack.scale(look.scaleX(), look.scaleY(), look.scaleZ());
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity) {
        CharacterLook look = CharacterLooks.get(entity.npcId());
        if (look != null && look.skin() != null) return look.skin();
        return SKINS.computeIfAbsent(entity.npcId(), id -> {
            ResourceLocation own = HxHMod.id("textures/entity/npc/" + id + ".png");
            // PNJ créé dans une quête sans skin : apparence par défaut (ajouter textures/entity/npc/<id>.png)
            return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(own).isPresent()
                    ? own : DEFAULT;
        });
    }

    private static final ResourceLocation DEFAULT = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    private static final java.util.Map<String, ResourceLocation> SKINS = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        CharacterLooks.onReload(SKINS::clear);
    }
}
