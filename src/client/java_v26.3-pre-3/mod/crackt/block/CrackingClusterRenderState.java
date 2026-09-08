package mod.crackt.block;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Render state for the cracking cluster core.
 */
public class CrackingClusterRenderState extends BlockEntityRenderState {
	BlockState displayState;
	float scale = 1.0f;
	int light = 0;
	@Nullable ClientLevel level;
	net.minecraft.world.phys.Vec3 offset = net.minecraft.world.phys.Vec3.ZERO;
	/** Baked model of {@link #displayState}, resolved during extraction. */
	final BlockModelRenderState blockModel = new BlockModelRenderState();
}
