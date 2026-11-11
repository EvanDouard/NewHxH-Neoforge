package com.evandouard.hxh.client;

import com.evandouard.hxh.exam.ExamCandidate;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Candidats de l'Examen : modèle du joueur et skins par défaut de Minecraft (Steve, Alex, Ari…). */
public class ExamCandidateRenderer extends HumanoidMobRenderer<ExamCandidate, PlayerModel<ExamCandidate>> {
    private static final String[] SKINS = {"steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri"};

    public ExamCandidateRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ExamCandidate entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/" + SKINS[entity.skin() % SKINS.length] + ".png");
    }
}
