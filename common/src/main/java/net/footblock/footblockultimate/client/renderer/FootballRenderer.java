package net.footblock.footblockultimate.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.footblock.footblockultimate.FootblockUltimate;
import net.footblock.footblockultimate.client.model.FootballEntityModel;
import net.footblock.footblockultimate.entity.FootballEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class FootballRenderer extends EntityRenderer<FootballEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            FootblockUltimate.MOD_ID, "textures/entity/football.png");

    private final FootballEntityModel model;

    public FootballRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FootballEntityModel(context.bakeLayer(FootballEntityModel.LAYER_LOCATION));
    }

    @Override
    public void render(FootballEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Lift the ball up by 0.25 blocks so its bottom sits on the ground
        poseStack.translate(0.0D, 0.25D, 0.0D);

        this.model.setupAnim(entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(this.getTextureLocation(entity)));
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FootballEntity entity) {
        return TEXTURE;
    }
}
