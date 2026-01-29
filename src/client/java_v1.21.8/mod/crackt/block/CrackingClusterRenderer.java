package mod.crackt.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import mod.crackt.ClientDisplayCache;

/**
 * Renders the cracking ore core by scaling the original ore's model uniformly.
 * 1.21.8 renderer uses the classic BlockEntityRenderer API with Vec3 camera parameter.
 */
public class CrackingClusterRenderer implements BlockEntityRenderer<CrackingClusterBlockEntity> {
	private final BlockRenderDispatcher dispatcher;

	public CrackingClusterRenderer(BlockEntityRendererProvider.Context ctx) {
		this.dispatcher = ctx.getBlockRenderDispatcher();
	}

	@Override
	public void render(CrackingClusterBlockEntity cluster, float tickDelta, PoseStack pose, MultiBufferSource buffers, int light, int overlay, Vec3 cameraPos) {
		BlockState display = cluster.getDisplayState();
		if (display == null) {
			BlockState cached = ClientDisplayCache.get(cluster.getBlockPos());
			if (cached != null) display = cached;
		}
		if (display == null) {
			display = cluster.getDisplayOrDefault();
		}

		pose.pushPose();
		Vec3 off = cluster.getDisplayOffset();
		if (!off.equals(Vec3.ZERO)) {
			pose.translate(off.x, off.y, off.z);
		}
		pose.translate(0.5, 0.5, 0.5);
		float scale = scaleFor(cluster);
		pose.scale(scale, scale, scale);
		pose.translate(-0.5, -0.5, -0.5);

		dispatcher.renderSingleBlock(display, pose, buffers, light, OverlayTexture.NO_OVERLAY);
		pose.popPose();
	}

	@Override
	public boolean shouldRender(CrackingClusterBlockEntity blockEntity, Vec3 cameraPos) {
		return true;
	}

	private float scaleFor(CrackingClusterBlockEntity cluster) {
		BlockState state = cluster.getBlockState();
		int stage = state.getValue(CrackingClusterBlock.STAGE);
		int stages = state.getValue(CrackingClusterBlock.STAGES);
		return CrackingClusterBlock.scaleFor(stage, stages);
	}
}
