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
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Uses the underlying ore's block state for hit/break particles on the cracking cluster.
 * Without this, particles would use the placeholder model (iron).
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
	@Shadow @Final private ClientLevel level;

	// Destroy (block break) particles: swap the state argument
	@ModifyVariable(method = "destroy", at = @At("HEAD"), argsOnly = true)
	private BlockState crackt$useDisplayStateOnDestroy(BlockState state, BlockPos pos) {
		return replaceIfCluster(state, pos);
	}

	// Hit (crack) particles: redirect the state lookup
	@Redirect(method = "crack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState crackt$useDisplayStateOnCrack(ClientLevel level, BlockPos pos, Direction direction) {
		return replaceIfCluster(level.getBlockState(pos), pos);
	}

	private BlockState replaceIfCluster(BlockState original, BlockPos pos) {
		if (original.is(CracktBlocks.CRACKING_CLUSTER)) {
			if (level.getBlockEntity(pos) instanceof CrackingClusterBlockEntity cluster) {
				BlockState display = cluster.getDisplayState();
				if (display != null) return display;
			}
			BlockState cached = ClientDisplayCache.get(pos);
			if (cached != null) return cached;
		}
		return original;
	}
}
