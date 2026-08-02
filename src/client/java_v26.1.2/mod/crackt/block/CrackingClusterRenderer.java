package mod.crackt.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import mod.crackt.ClientDisplayCache;

/**
 * Renders the cracking ore core by scaling the original ore's model uniformly.
 */
public class CrackingClusterRenderer implements BlockEntityRenderer<CrackingClusterBlockEntity, CrackingClusterRenderState> {
	private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

	private final BlockModelResolver blockModelResolver;

	public CrackingClusterRenderer(BlockEntityRendererProvider.Context context) {
		this.blockModelResolver = context.blockModelResolver();
	}

	@Override
	public CrackingClusterRenderState createRenderState() {
		return new CrackingClusterRenderState();
	}

	@Override
	public void extractRenderState(CrackingClusterBlockEntity cluster, CrackingClusterRenderState state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
		BlockEntityRenderer.super.extractRenderState(cluster, state, partialTick, camera, overlay);
		BlockState display = cluster.getDisplayState();
		if (display == null) {
			BlockState cached = ClientDisplayCache.get(cluster.getBlockPos());
			if (cached != null) display = cached;
		}
		state.displayState = display;
		state.scale = scaleFor(cluster);
		if (cluster.getLevel() != null) {
			state.light = LevelRenderer.getLightCoords(cluster.getLevel(), cluster.getBlockPos());
		}
		state.level = cluster.getLevel() instanceof ClientLevel clientLevel ? clientLevel : null;
		state.offset = cluster.getDisplayOffset();
		if (display != null) {
			this.blockModelResolver.update(state.blockModel, display, BLOCK_DISPLAY_CONTEXT);
		} else {
			state.blockModel.clear();
		}
	}

	@Override
	public void submit(CrackingClusterRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState cameraState) {
		if (state.displayState == null) return;
		pose.pushPose();
		if (!state.offset.equals(net.minecraft.world.phys.Vec3.ZERO)) {
			pose.translate(state.offset.x, state.offset.y, state.offset.z);
		}
		pose.translate(0.5, 0.5, 0.5);
		pose.scale(state.scale, state.scale, state.scale);
		pose.translate(-0.5, -0.5, -0.5);

		state.blockModel.submit(pose, collector, state.light, OverlayTexture.NO_OVERLAY, 0);

		if (state.breakProgress != null && state.level != null) {
			ModelFeatureRenderer.CrumblingOverlay breakOverlay = state.breakProgress;
			BlockStateModel breakModel = Minecraft.getInstance().getModelManager()
				.getBlockStateModelSet().get(state.displayState);
			collector.submitBreakingBlockModel(pose, breakModel, state.displayState.getSeed(state.blockPos), breakOverlay.progress());
		}
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
