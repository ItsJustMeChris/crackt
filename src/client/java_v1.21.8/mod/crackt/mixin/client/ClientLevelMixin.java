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
 * Ensures block break particles use the cracking cluster's display state.
 * 1.21.8 only has addDestroyBlockEffect (no addBreakingBlockEffect).
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@ModifyVariable(method = "addDestroyBlockEffect", at = @At("HEAD"), argsOnly = true)
	private BlockState crackt$swapDestroyState(BlockState state, BlockPos pos) {
		return crackt$swapState((ClientLevel)(Object)this, state, pos);
	}

	private static BlockState crackt$swapState(ClientLevel level, BlockState original, BlockPos pos) {
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
