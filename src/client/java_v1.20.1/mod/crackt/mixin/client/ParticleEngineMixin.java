package mod.crackt.mixin.client;

import mod.crackt.CracktBlocks;
import mod.crackt.ClientDisplayCache;
import mod.crackt.block.CrackingClusterBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 1.20.1 lacks addBreakingBlockEffect; hook particle crack instead.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
	@Shadow @Final protected ClientLevel level;

	@ModifyVariable(
		method = "crack",
		at = @At(value = "STORE"),
		ordinal = 0,
		require = 0,
		expect = 0
	)
	private BlockState crackt$swapCrackState(BlockState original, BlockPos pos, Direction side) {
		if (original.is(CracktBlocks.CRACKING_CLUSTER)) {
			if (level.getBlockEntity(pos) instanceof CrackingClusterBlockEntity cluster) {
				BlockState display = cluster.getDisplayState();
				if (display == null) {
					display = ClientDisplayCache.get(pos);
				}
				if (display == null) {
					display = cluster.getDisplayOrDefault();
				}
				return display;
			}
			BlockState cached = ClientDisplayCache.get(pos);
			if (cached != null) return cached;
			return net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState(); // safe fallback to avoid missing model
		}
		return original;
	}
}
