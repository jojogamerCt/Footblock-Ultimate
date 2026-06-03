package net.footblock.footblockultimate.client.model;

import net.footblock.footblockultimate.FootblockUltimate;
import net.footblock.footblockultimate.entity.FootballEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class FootballEntityModel extends HierarchicalModel<FootballEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(FootblockUltimate.MOD_ID, "football"), "main");

    private final ModelPart root;
    private final ModelPart ball;

    public FootballEntityModel(ModelPart root) {
        this.root = root;
        this.ball = root.getChild("ball");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        CubeListBuilder builder = CubeListBuilder.create();

        // Center cube: size 6x6x6, texture offset (0, 0)
        builder.texOffs(0, 0).addBox(-3.0f, -3.0f, -3.0f, 6.0f, 6.0f, 6.0f, CubeDeformation.NONE);

        // Caps of size 4x4x1 or 1x4x4 or 4x1x4:
        // Top Cap (Up): size 4x1x4, texture offset (0, 20)
        builder.texOffs(0, 20).addBox(-2.0f, -4.0f, -2.0f, 4.0f, 1.0f, 4.0f, CubeDeformation.NONE);

        // Bottom Cap (Down): size 4x1x4, texture offset (0, 25)
        builder.texOffs(0, 25).addBox(-2.0f, 3.0f, -2.0f, 4.0f, 1.0f, 4.0f, CubeDeformation.NONE);

        // North Cap (Front): size 4x4x1, texture offset (16, 20)
        builder.texOffs(16, 20).addBox(-2.0f, -2.0f, -4.0f, 4.0f, 4.0f, 1.0f, CubeDeformation.NONE);

        // South Cap (Back): size 4x4x1, texture offset (16, 25)
        builder.texOffs(16, 25).addBox(-2.0f, -2.0f, 3.0f, 4.0f, 4.0f, 1.0f, CubeDeformation.NONE);

        // West Cap (Left): size 1x4x4, texture offset (32, 20)
        builder.texOffs(32, 20).addBox(-4.0f, -2.0f, -2.0f, 1.0f, 4.0f, 4.0f, CubeDeformation.NONE);

        // East Cap (Right): size 1x4x4, texture offset (32, 25)
        builder.texOffs(32, 25).addBox(3.0f, -2.0f, -2.0f, 1.0f, 4.0f, 4.0f, CubeDeformation.NONE);

        partdefinition.addOrReplaceChild("ball", builder, PartPose.offset(0.0f, 0.0f, 0.0f));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(FootballEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.ball.xRot = entity.getRollX();
        this.ball.yRot = entity.getRollY();
        this.ball.zRot = entity.getRollZ();
    }
}
