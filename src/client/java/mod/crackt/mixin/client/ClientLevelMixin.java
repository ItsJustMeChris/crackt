package mod.crackt.mixin.client;

import mod.crackt.CracktBlocks;
import mod.crackt.ClientDisplayCache;
import mod.crackt.block.CrackingClusterBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Ensures block break/hit particles use the cracking cluster's display state.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@ModifyVariable(method = "addDestroyBlockEffect", at = @At("HEAD"), argsOnly = true)
	private BlockState crackt$swapDestroyState(BlockState state, BlockPos pos) {
		return swapState((ClientLevel)(Object)this, state, pos);
	}

	@ModifyVariable(method = "addBreakingBlockEffect", at = @At("HEAD"), argsOnly = true)
	private BlockState crackt$swapHitState(BlockState state, BlockPos pos) {
		return swapState((ClientLevel)(Object)this, state, pos);
	}

	private static BlockState swapState(ClientLevel level, BlockState original, BlockPos pos) {
		if (original.is(CracktBlocks.CRACKING_CLUSTER)) {
			if (level.getBlockEntity(pos) instanceof CrackingClusterBlockEntity cluster && cluster.getDisplayState() != null) {
				return cluster.getDisplayState();
			}
			BlockState cached = ClientDisplayCache.get(pos);
			if (cached != null) return cached;
		}
		return original;
	}
}
