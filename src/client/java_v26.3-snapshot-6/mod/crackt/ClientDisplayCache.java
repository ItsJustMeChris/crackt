package mod.crackt;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small client-side cache of cracking cluster display states/offsets so particles
 * can keep using the correct ore even if the block entity isn't loaded yet.
 */
public final class ClientDisplayCache {
	private static final Map<BlockPos, BlockState> STATES = new ConcurrentHashMap<>();

	private ClientDisplayCache() {}

	public static void put(BlockPos pos, BlockState state) {
		if (pos == null || state == null) return;
		STATES.put(pos.immutable(), state);
	}

	public static BlockState get(BlockPos pos) {
		return pos == null ? null : STATES.get(pos);
	}

	public static void clear() {
		STATES.clear();
	}
}
